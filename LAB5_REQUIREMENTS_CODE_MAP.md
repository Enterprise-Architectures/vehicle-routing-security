# Lab 5 — RESTful Services: implementation, explanation and evidence

**Course:** Enterprise Architectures / Java Technologies · **academic year:** 2026–2027  
**Application:** Vehicle Routing Optimization System (VROS)  
**Scope:** Compulsory (1p) retained from earlier work; Homework (2p) implemented here. Advanced is not claimed.

## How to run

The Spring Boot application uses Java 21+ and PostgreSQL. The existing WSL2 Docker database
is `vros-lab4-postgres` on localhost port **5434**, with database `vros_lab4`.
Port **8081** is used for HTTP; port 8080 is occupied by a separate project.

```bash
docker ps --filter name=vros-lab4-postgres
mvn test
mvn spring-boot:run
# In another terminal:
python3 scripts/test_lab5_api.py
```

**Important:** Vehicle POST requires **exactly one existing depot** (lab assumption).
If the database is empty, start once with `mvn spring-boot:run -Dspring-boot.run.arguments=--vros.demo.enabled=true`
to insert the idempotent Lab 4 example depot, then use normal startup.
If the database has multiple depots, clean up/choose a single depot for the homework exercise.

## Architecture: why the files have these roles

```text
HTTP client / Swagger UI / curl
   |
   v
web/CustomerController.java                 <- existing compulsory CRUD
web/{Vehicle,Delivery,Scenario}Controller.java <- CRUD, DTOs, @Valid, HTTP status
web/ApiExceptionHandler.java                <- uniform errors
web/ETags.java                               <- conditional HTTP GET
   |
   v
service/{Customer,Vehicle,Delivery,Scenario}Service.java
       -> business rules + @Transactional + ID resolution
   |
   v
repository/*Repository.java                 <- Spring Data JPA
   |
   v
model/{Customer,Vehicle,Delivery,Scenario}.java
       -> JPA mappings; schema.sql -> PostgreSQL
```

**Separation of concerns:** Controllers work with HTTP and DTOs; services enforce
rules and transaction boundaries; repositories query the database; entities model
relationships. Returning JPA entities directly in HTTP would expose internal
relations and may cause infinite JSON recursion or LazyInitializationException.

## Compulsory (1 point)

**"Implement CRUD REST endpoints for customers."** Implemented in
`web/CustomerController.java` already: GET list, GET by ID, POST, PUT, DELETE.
Uses `CustomerService` and a request/response DTO. The existing controller was
extended with conditional GET for the homework, without removing its CRUD.

**"Test them using curl or Postman."** `scripts/test_lab5_api.py` executes actual
HTTP requests against *all* four resources, including customer CRUD. A direct curl
sequence is included below.

## Homework — bullet 1 (domain model / many-to-many)

> Each scenario has a name and a set of vehicles and deliveries.
> Include at least one many-to-many relationship.

**WHAT:** `model/Scenario.java` defines:
- id (database-generated primary key);
- name (required, unique);
- `Set<Vehicle> vehicles`: `@ManyToMany` + `@JoinTable(scenario_vehicles)`;
- `Set<Delivery> deliveries`: `@ManyToMany` + `@JoinTable(scenario_deliveries)`.

**HOW:** `schema.sql` creates `scenarios`, `scenario_vehicles`, and
`scenario_deliveries`. Both join tables have compound primary keys, preventing
duplicate membership. Their member foreign keys are RESTRICT and the scenario
foreign keys CASCADE. This ensures a scenario is safely deleted without deleting
vehicles or deliveries.

**WHY:** One van can participate in multiple "what-if" scenarios; one scenario
can use many vans. The same is true for deliveries. This is many-to-many, not
one-to-many. We do NOT compute routes yet: a scenario is a *problem definition*,
and route optimization will belong to a future lab.

For example:

```text
Scenario 17: "Morning"
  vehicles:  [3, 5]
  deliveries: [11, 12]

Scenario 18: "Afternoon"
  vehicles:  [3]       <- shared vehicle 3
  deliveries: [12]     <- shared delivery 12

scenario_vehicles:       scenario_deliveries:
(17,3), (17,5), (18,3)   (17,11), (17,12), (18,12)
```

## Homework — bullet 2 (REST CRUD)

**WHAT:** The three new controllers are:
`web/VehicleController.java`, `web/DeliveryController.java`,
`web/ScenarioController.java`. The customer controller covers compulsory.

| Resource | List | Read | Create | Update | Delete |
|---|---|---|---|---|---|
| customers | GET /api/customers | GET /api/customers/{id} | POST /api/customers | PUT /api/customers/{id} | DELETE /api/customers/{id} |
| vehicles | GET /api/vehicles | GET /api/vehicles/{id} | POST /api/vehicles | PUT /api/vehicles/{id} | DELETE /api/vehicles/{id} |
| deliveries | GET /api/deliveries | GET /api/deliveries/{id} | POST /api/deliveries | PUT /api/deliveries/{id} | DELETE /api/deliveries/{id} |
| scenarios | GET /api/scenarios | GET /api/scenarios/{id} | POST /api/scenarios | PUT /api/scenarios/{id} | DELETE /api/scenarios/{id} |

**HOW:** Controllers delegate to `*Service` and `JpaRepository`.
POST returns `201 Created` and a `Location` header; GET/PUT return `200`;
DELETE returns `204 No Content`; absent records yield `404`.

**ASSUMPTIONS:** The REST vehicle request has no `driverId` or `depotId`.
`VehicleService.createDriverless` uses the one available depot and passes a null
driver to the existing Lab 4 creation method. The Lab 4 `Driver` class remains
for backward compatibility, but is not needed for Lab 5. A delivery references
an existing customer by `customerId`. Its `registeredAt` is generated by the
server and preserved by PUT. A scenario PUT *replaces*, not merges, member sets.

## Homework — bullet 3 (DTOs, validation, custom exception)

**WHAT:** Each controller declares `*Request` and `*Response` records.
A DTO describes the *network representation*, not a database row.
The request records use `@NotBlank`, `@NotNull`, `@Positive`, `@Size`,
and container element validation for scenario member IDs.

**HOW:** `@Valid @RequestBody` runs Bean Validation before the controller calls
a service. Invalid fields return HTTP `400` through
`web/ApiExceptionHandler.java`. Services throw
`ResourceNotFoundException` (custom runtime exception, HTTP 404) or
`BadRequestException` (invalid scenario IDs, HTTP 400). A database uniqueness or
foreign-key conflict produces `409`, without leaking SQL internals.

**WHY:** DTOs protect encapsulation; validation catches malformed user input early;
the advice converts domain errors to stable HTTP semantics. A controller should
never return a stack trace for an ordinary missing ID.

Example rejected request:
```json
{"name":"","vehicleIds":[-5],"deliveryIds":null}
```
This produces HTTP 400 rather than creating an invalid scenario.

## Homework — bullet 4 (ETag / If-None-Match)

**WHAT:** Conditional GET is implemented for
`GET /api/customers/{id}` and `GET /api/scenarios/{id}`.

**HOW:** `ETags.weak(dto)` computes a SHA-256 digest of the deterministic DTO
value. `WebRequest.checkNotModified(etag)` interprets `If-None-Match`
(including weak validators and tag lists). An unchanged resource receives
`304 Not Modified` with no body. When the DTO changes, its ETag changes and
the server returns `200` with the fresh representation.

**WHY:** A cache/client can avoid repeatedly downloading identical resources.
Weak ETags are used because JSON and XML are semantically the same data, but
their exact bytes are different. ETag is *not* a login token or a data lock.

```bash
curl -i http://localhost:8081/api/scenarios/17
# ETag: W/"..." (copy the complete header value)

curl -i -H 'If-None-Match: W/"<digest copied from above>"' \
  http://localhost:8081/api/scenarios/17
# Expected: HTTP/1.1 304, no body, if unchanged
```

## Homework — bullet 5 (JSON/XML negotiation)

**WHAT:** `GET /api/scenarios/{id}` supports both media types.

**HOW:** `ScenarioController.one` declares
`produces = {application/json, application/xml}`. Adding
`tools.jackson.dataformat:jackson-dataformat-xml` to `pom.xml`
registers an XML message converter in Spring Boot 4. The client selects
a representation using the **Accept** header; controllers return the same DTO.

```bash
curl -i -H 'Accept: application/json' http://localhost:8081/api/scenarios/17
curl -i -H 'Accept: application/xml'  http://localhost:8081/api/scenarios/17
```

The endpoint sends `Vary: Accept` to inform intermediaries about content
negotiation. Neither JSON nor XML requires a separate URL or duplicated service.

## Homework — bullet 6 (Springdoc OpenAPI and Swagger UI)

**WHAT:** `ScenarioController` has `@Tag`, `@Operation`, and
`@ApiResponse` on the resource operations.

**HOW:** `pom.xml` adds Springdoc starter WebMVC UI **3.2.0**, compatible
with Spring Boot **4.1.x**. Springdoc discovers the controllers and annotations
and publishes:

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8081/v3/api-docs

**WHY:** Swagger UI renders a browsable contract and lets the examiner execute
GET/POST/PUT/DELETE requests interactively. It is not a replacement for tests.

## Reproducible HTTP demonstration with curl

Run these commands sequentially (change IDs to the values returned by POST):

```bash
# 1. Customer - prerequisite for delivery
curl -i -H 'Content-Type: application/json' -d \
  '{"name":"Sample Customer","email":"sample-vros@example.test","phone":"0712345678","address":"Iasi","coordinates":"47.17,27.58"}' \
  http://localhost:8081/api/customers

# 2. Vehicle - uses the single database depot automatically
curl -i -H 'Content-Type: application/json' -d \
  '{"name":"Lab 5 Van","capacity":100,"available":true}' \
  http://localhost:8081/api/vehicles

# 3. Delivery - replace CUSTOMER_ID
curl -i -H 'Content-Type: application/json' -d \
  '{"customerId":CUSTOMER_ID,"quantity":20,"notes":"Books"}' \
  http://localhost:8081/api/deliveries

# 4. Scenario - replace VEHICLE_ID and DELIVERY_ID
curl -i -H 'Content-Type: application/json' -d \
  '{"name":"Morning schedule","vehicleIds":[VEHICLE_ID],"deliveryIds":[DELIVERY_ID]}' \
  http://localhost:8081/api/scenarios

# 5. GET / PUT / DELETE; replace SCENARIO_ID
curl -i http://localhost:8081/api/scenarios/SCENARIO_ID
curl -i -X PUT -H 'Content-Type: application/json' -d \
  '{"name":"Morning schedule updated","vehicleIds":[VEHICLE_ID],"deliveryIds":[DELIVERY_ID]}' \
  http://localhost:8081/api/scenarios/SCENARIO_ID
curl -i -X DELETE http://localhost:8081/api/scenarios/SCENARIO_ID
```

For a **fully automatic** HTTP demonstration with unique test names, schema
validation, ETag 304, XML, Swagger, and cleanup:

```bash
python3 scripts/test_lab5_api.py
```

## Submission checklist / 2-point homework coverage

| Requirement | Code | Test / visible evidence |
|---|---|---|
| Scenario, many-to-many | `Scenario.java`, `schema.sql` | POST scenario + GET shows member IDs; join-table SQL |
| CRUD for vehicles, deliveries, scenarios | controllers + services | smoke script creates/updates/gets/deletes all |
| DTO + Bean Validation | nested records, `@Valid` | malformed POST gives 400 |
| Custom runtime exception + handling | `ResourceNotFoundException`, `ApiExceptionHandler` | GET unknown ID gives 404 |
| ETag + If-None-Match | `ETags`, conditional GET | 200 ETag then 304 |
| JSON + XML | Scenario GET + Maven Jackson XML | Accept headers return two content types |
| OpenAPI + Swagger | Springdoc dependency + annotations | UI and `/v3/api-docs` load |

**Advanced (Flyway, HATEOAS, versioning): not implemented or claimed.**
Schema creation currently uses the Lab 4 idempotent `schema.sql`; this is
*not* versioned database migration.
