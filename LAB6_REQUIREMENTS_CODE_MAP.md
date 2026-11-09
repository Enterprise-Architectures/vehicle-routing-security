# Enterprise Architectures — Lab 6 (Security)

**Base:** Lab 5 VROS REST API; original project left unchanged.
**Implementation:** Spring Boot 4.1.1 / Spring Security 7.1.x / PostgreSQL 17.
**Scope:** Compulsory (1p) + Homework (2p). Advanced is deliberately excluded.

## 1. Requirement → source mapping

| Requirement | Source | Explanation |
|---|---|---|
| Add Spring Security | `pom.xml` | `spring-boot-starter-security` and `spring-boot-starter-security-oauth2-resource-server` |
| Verify all endpoints protected | `security/CompulsorySecurityConfig.java` | `anyRequest().authenticated()` |
| Mock `/login` | `web/MockLoginController.java` | `@Profile("compulsory")`, POST returns a JSON message |
| Permit only `/login` anonymously | `security/CompulsorySecurityConfig.java` | The only `permitAll()` matcher in the compulsory profile |
| Driver/customer accounts | `model/AppUser.java`, `model/Role.java` | AppUser has optional unique OneToOne FK to exactly one Customer or Driver; ADMIN has neither |
| Read users/roles from database | `repository/AppUserRepository.java`, `security/DatabaseAuthenticationConfig.java` | UserDetailsService retrieves BCrypt hash and role from PostgreSQL |
| JWT login | `web/AuthController.java`, `service/JwtAuthService.java`, `security/JwtConfiguration.java` | Authenticates with DB, signs HS256 JWT; one-hour access token, validates issuer/signature/expiration |
| RBAC for POST/PUT/DELETE | `security/ProductionSecurityConfig.java` | Only admins can mutate vehicles, customers, scenarios; customers can create deliveries for themselves |
| Public GETs where reasonable | `security/ProductionSecurityConfig.java` | Vehicles and scenarios public; customer directory and deliveries protected |
| BCrypt | `security/DatabaseAuthenticationConfig.java` | BCrypt strength 12; only hash stored |
| Registration | `web/AuthController.java`, `service/AccountService.java` | Self-registration creates a linked CUSTOMER account; never permits self-assigned ADMIN/DRIVER |
| Driver provisioning | `web/AdminUserController.java` | ADMIN creates DRIVER account linked to an existing driver |
| `@PreAuthorize` | `service/DeliveryService.java`, `web/VehicleController.java`, `web/AdminUserController.java` | Ownership check and ADMIN checks, even on service calls |
| Method-security tests | `src/test/java/ro/uaic/vros/security/MethodSecurityTest.java` | Anonymous/customer-other/customer-own/driver/admin cases |
| Dev bypass | `security/DevSecurityConfig.java` | `permitAll`, no `@EnableMethodSecurity` in dev |
| Production | `security/ProductionSecurityConfig.java` | Stateless bearer authentication + RBAC + method security |
| Database schema | `src/main/resources/schema.sql` | `app_users` with unique FKs and role/profile integrity constraints |

## 2. Profiles and how to run them

**Database:** Uses a dedicated `vros_lab6` database, not the existing Lab 4/5 database.
The Lab 6 Compose service provides PostgreSQL 17 at localhost:5435.
**API:** localhost:8081 by default; override with `--server.port=8186` if occupied.

In project directory:

```powershell
# Compulsory: only /login is anonymous
mvn spring-boot:run "-Dspring-boot.run.profiles=compulsory"
python scripts/test_lab6_api.py --profile compulsory --base http://localhost:8081

# Development: authentication and method authorization bypassed (local only!)
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
python scripts/test_lab6_api.py --profile dev --base http://localhost:8081

# Production: create a cryptographically random signing secret
$key = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($key)
$env:VROS_JWT_SECRET_BASE64 = [Convert]::ToBase64String($key)
$env:VROS_ADMIN_USERNAME = 'admin'
$env:VROS_ADMIN_PASSWORD = 'Choose-a-new-strong-admin-password!'
mvn spring-boot:run "-Dspring-boot.run.profiles=prod"
python scripts/test_lab6_api.py --profile prod --base http://localhost:8081
```

**Production startup fails intentionally** if VROS_JWT_SECRET_BASE64 is missing/too short.
The admin is bootstrapped only if both environment variables are provided and
the admin does not already exist. Never expose the `dev` profile or commit secrets.

### HTTP sequence to present

1. Start `compulsory`; POST `/login` returns 200; GET `/api/vehicles` returns 401.
2. Start `prod`; GET `/api/vehicles` is 200; POST `/api/vehicles` without JWT is 401.
3. POST `/register` with username/password/contact/address creates customer + linked account (201).
4. POST `/login` returns JWT. Send `Authorization: Bearer <accessToken>`.
5. Customer's POST `/api/vehicles` gives 403; own delivery gives 201, another customer's 403.
6. Admin can mutate all resources and create DRIVER accounts.
7. Start `dev`; POST reaches controller without token; validation errors are 400, not 401/403.

### Role permissions

| Endpoint | Anonymous | CUSTOMER | DRIVER | ADMIN |
|---|---|---|---|---|
| POST /login, /register | Yes | Yes | Yes | Yes |
| GET /api/vehicles, /api/scenarios | Yes | Yes | Yes | Yes |
| GET /api/customers | 401 | 403 | 403 | 200 |
| GET /api/deliveries | 401 | 403 | 200 | 200 |
| POST /api/deliveries | 401 | own customer only | 403 | 201 |
| Other POST/PUT/DELETE /api/** | 401 | 403 | 403 | Allowed |

### Notes / limits

- JWT access tokens expire after 1 hour; no refresh token or Keycloak (Advanced tasks).
- Stateless tokens carry the role assigned at issuance; changing a role will not revoke old
  tokens until expiry. Production deployments should add revocation/key rotation and TLS.
- Compulsory profile is a *separate* specification: its login is intentionally mock-only;
  homework profile additionally permits `/register` and public vehicle/scenario GETs.
- Customer email/phone are deliberately not publicly readable.
- To demonstrate DRIVER, first create a driver row in the existing domain, then provision
  an account at `POST /api/admin/users/drivers` with an ADMIN bearer token.
