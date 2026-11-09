#!/usr/bin/env python3
"""Lab 6 HTTP authorization smoke test; requires a running VROS server.

Examples:
    python scripts/test_lab6_api.py --profile compulsory --base http://localhost:8186
    python scripts/test_lab6_api.py --profile prod --base http://localhost:8186
    python scripts/test_lab6_api.py --profile dev --base http://localhost:8186
"""
import argparse
import json
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen

parser = argparse.ArgumentParser()
parser.add_argument("--profile", required=True, choices=("compulsory", "prod", "dev"))
parser.add_argument("--base", default="http://localhost:8186")
args = parser.parse_args()
checks = 0

def request(method, path, body=None, token=None):
    data = None if body is None else json.dumps(body).encode("utf-8")
    headers = {"Accept": "application/json"}
    if data is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = Request(args.base.rstrip("/") + path, data=data, headers=headers, method=method)
    try:
        with urlopen(req, timeout=15) as response:
            payload = response.read().decode("utf-8")
            return response.status, json.loads(payload) if payload else {}
    except HTTPError as e:
        payload = e.read().decode("utf-8")
        try:
            parsed = json.loads(payload)
        except ValueError:
            parsed = payload[:300]
        return e.code, parsed

def expect(title, method, path, status, body=None, token=None):
    global checks
    actual, result = request(method, path, body, token)
    if actual != status:
        raise AssertionError(f"{title}: expected {status}, received {actual}; {str(result)[:350]}")
    checks += 1
    print(f"PASS {checks:02d}: {title} ({actual})")
    return result

if args.profile == "compulsory":
    expect("mock login public", "POST", "/login", 200, {})
    expect("vehicles GET protected", "GET", "/api/vehicles", 401)
    expect("vehicles POST protected", "POST", "/api/vehicles", 401, {})
    expect("Swagger protected", "GET", "/swagger-ui/index.html", 401)
elif args.profile == "dev":
    expect("GET vehicles public", "GET", "/api/vehicles", 200)
    expect("GET customers public in dev", "GET", "/api/customers", 200)
    expect("POST vehicles bypasses authentication and reaches validation",
           "POST", "/api/vehicles", 400, {"name": "", "capacity": 0, "available": True})
else:
    expect("vehicles GET public", "GET", "/api/vehicles", 200)
    expect("scenarios GET public", "GET", "/api/scenarios", 200)
    expect("customers GET protected", "GET", "/api/customers", 401)
    expect("deliveries GET protected", "GET", "/api/deliveries", 401)
    expect("write without token rejected", "POST", "/api/vehicles", 401, {})
    key = uuid.uuid4().hex[:12]
    username = "lab6_" + key
    registration = dict(username=username, password="TestPassword_2026!",
        name="Lab 6 Customer", email=f"{username}@example.test", phone="+40700000000",
        address="Test address", coordinates="47.16,27.58")
    created = expect("customer registration", "POST", "/register", 201, registration)
    assert created["role"] == "CUSTOMER" and created["customerId"] > 0
    checks += 1
    print(f"PASS {checks:02d}: customer linked to login")
    expect("duplicate username rejected", "POST", "/register", 409, registration)
    expect("bad password rejected", "POST", "/login", 401,
           {"username": username, "password": "incorrect"})
    auth = expect("JWT login", "POST", "/login", 200,
                  {"username": username, "password": registration["password"]})
    assert auth["tokenType"] == "Bearer" and auth["accessToken"].count(".") == 2
    token = auth["accessToken"]
    expect("customer cannot create vehicle", "POST", "/api/vehicles", 403,
           {"name": "test", "capacity": 1, "available": True}, token)
    expect("customer cannot view private customer directory", "GET", "/api/customers", 403, token=token)
    expect("customer cannot provision driver", "POST", "/api/admin/users/drivers", 403,
           {"username": "notallowed", "password": "SomePassword_2026", "driverId": 1}, token)
    expect("customer cannot read all deliveries", "GET", "/api/deliveries", 403, token=token)
    expect("customer cannot submit another customer's delivery", "POST", "/api/deliveries", 403,
           {"customerId": created["customerId"] + 999, "quantity": 2}, token)
    delivered = expect("customer can submit own delivery", "POST", "/api/deliveries", 201,
                       {"customerId": created["customerId"], "quantity": 2,
                        "notes": "Lab 6 role-based delivery"}, token)
    assert delivered["customerId"] == created["customerId"]
    checks += 1
    print(f"PASS {checks:02d}: delivery ownership matched")
print(f"SUCCESS: {checks} checks, profile {args.profile}")
