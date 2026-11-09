# Enterprise Architectures — Lab 6: Spring Security

VROS REST API extended from [Lab 5](https://github.com/Enterprise-Architectures/vehicle-routing-rest-api).
Covers **Compulsory (1p)** and **Homework (2p)**; Advanced is out of scope.

## Tech stack

Java 21 target (builds with JDK 25), Spring Boot 4.1.1, Spring Security 7.1,
Spring Data JPA, PostgreSQL 17, BCrypt, signed HS256 JWT.

## Quick start (Windows PowerShell)

```powershell
# From the project directory; Docker must be running.
docker compose up -d
mvn test
```

The Compose service creates a dedicated `vros_lab6` database on **127.0.0.1:5435**.
To run against a different database, set `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USER`, and `DB_PASSWORD`. The SQL initializer creates tables without dropping data.
Spring Boot serves on **8081** by default.

### Compulsory: demonstrate everything protected except /login

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=compulsory"
# In another terminal:
python scripts/test_lab6_api.py --profile compulsory --base http://localhost:8081
```

A mock `POST /login` returns 200; unauthenticated `GET /api/vehicles`
and `POST /api/vehicles` return 401. This profile intentionally does not
issue real tokens or permit registration.

### Homework: production security (default)

```powershell
# Generate an unpredictable 256-bit key before first use:
$secret = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($secret)
$env:VROS_JWT_SECRET_BASE64 = [Convert]::ToBase64String($secret)
$env:VROS_ADMIN_USERNAME = 'admin'
$env:VROS_ADMIN_PASSWORD = 'Replace-With-Your-Own-Long-Password!'
mvn spring-boot:run "-Dspring-boot.run.profiles=prod"
# In another terminal:
python scripts/test_lab6_api.py --profile prod --base http://localhost:8081
```

The signing secret is required in production, and startup fails without one.
Generate it **once**, store it securely, and reuse the same key for consistent
token validation across restarts. Do not commit the key or passwords.
The admin account is bootstrapped once if not already present; resetting it
requires explicit management (the environment variable is not a password reset).

### Development (authorization bypass)

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
python scripts/test_lab6_api.py --profile dev --base http://localhost:8081
```

This mode deliberately permits every request and disables method security,
even on mutation endpoints. **Never expose dev mode to the internet.**

## HTTP requests (prod)

Create an ordinary customer account:

```http
POST /register
Content-Type: application/json

{
  "username": "alice",
  "password": "A-Strong-Password-2026!",
  "name": "Alice",
  "email": "alice@example.test",
  "phone": "+40700000000",
  "address": "Iasi",
  "coordinates": "47.16,27.58"
}
```

Log in:

```http
POST /login
Content-Type: application/json

{"username":"alice","password":"A-Strong-Password-2026!"}
```

The response contains `accessToken`, `tokenType: Bearer`, and
`expiresInSeconds: 3600`. For protected endpoints send
`Authorization: Bearer <accessToken>` (never include the token in a URL).

| Permission | Anonymous | CUSTOMER | DRIVER | ADMIN |
|---|---|---|---|---|
| Login, customer registration | yes | yes | yes | yes |
| Read vehicles or scenarios | yes | yes | yes | yes |
| Read customer directory | 401 | 403 | 403 | yes |
| Read deliveries | 401 | 403 | yes | yes |
| Create delivery | 401 | own customer ID | 403 | yes |
| Modify customers/vehicles/scenarios | 401 | 403 | 403 | yes |
| Provision driver account | 401 | 403 | 403 | yes |

To provision a DRIVER account, insert a Driver through the VROS domain or
DB and call `POST /api/admin/users/drivers` as ADMIN with:
`{"username":"driver1","password":"StrongPassword2026!","driverId":1}`.
Only admins may issue DRIVER accounts; public registration always gives CUSTOMER.

The common `app_users` table has a role and one-to-one foreign key to
the corresponding `customers` or `drivers` table. Database constraints
disallow invalid combinations; the stored password is BCrypt-hashed.
JWT signature, issuer, and expiry are verified on every protected request.

## Tests and where to find things

- `scripts/test_lab6_api.py`: live HTTP smoke tests (prod / dev / compulsory).
- `src/test/java/ro/uaic/vros/security/MethodSecurityTest.java`: five
  `@PreAuthorize` cases (anonymous, foreign customer, own customer, driver, admin).
- `LAB6_REQUIREMENTS_CODE_MAP.md`: all requirements mapped to implementation.
- Existing Lab 4/5 domain, service, and HTTP tests remain part of the project.
- PostgreSQL integration tests are opt-in and may be skipped without their test setup.

**Security note:** This is a course implementation, not a production identity service.
No refresh-token rotation, token revocation, OIDC/Keycloak integration,
login rate-limiting, audit alerting, or password reset. Use HTTPS and strong
secrets in any real deployment.

### Optional PostgreSQL integration tests

```powershell
Set-Item Env:RUN_PG_INTEGRATION_TESTS true
Set-Item Env:SPRING_PROFILES_ACTIVE dev
mvn "-Dtest=PostgresRepositoryIntegrationTest" test
Remove-Item Env:RUN_PG_INTEGRATION_TESTS
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

These two tests exercise repository queries against the dedicated PostgreSQL
database, not an in-memory replacement. They passed with PostgreSQL 17.
