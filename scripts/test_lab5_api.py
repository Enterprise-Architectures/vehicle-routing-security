#!/usr/bin/env python3
"""
Lab 5 HTTP smoke test. Uses only Python's standard library.

WHAT: Exercises compulsory + homework against a *running* Spring Boot server.
WHY: Unlike unit tests, this verifies actual HTTP status codes, validation,
     Content-Type, ETag, Swagger and referential integrity end to end.
HOW: Unique names avoid collisions; finally deletes created resources in FK-safe order.

Usage:
    python3 scripts/test_lab5_api.py
    VROS_URL=http://localhost:8081 python3 scripts/test_lab5_api.py
Requires exactly one depot and a reachable PostgreSQL-backed application.
"""
import json
import os
import urllib.error
import urllib.request
import uuid
import xml.etree.ElementTree as ET

BASE = os.environ.get("VROS_URL", "http://localhost:8081").rstrip("/")
SUFFIX = uuid.uuid4().hex[:10]


def http(method, path, body=None, headers=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    h = {"Accept": "application/json"}
    if data is not None:
        h["Content-Type"] = "application/json"
    h.update(headers or {})
    request = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        response = urllib.request.urlopen(request, timeout=20)
    except urllib.error.HTTPError as error:
        response = error  # 304/400/404/409 are legitimate test outcomes
    raw = response.read()
    content_type = response.headers.get("Content-Type", "")
    parsed = json.loads(raw) if raw and "json" in content_type else raw.decode("utf-8")
    return response.status, dict(response.headers), parsed


def expect(status, actual, description):
    assert actual == status, f"{description}: expected {status}, got {actual}"
    print(f"PASS {description}: HTTP {status}")


def main():
    created = {}
    try:
        customer = {"name": "Lab5 Client " + SUFFIX, "email": f"lab5-{SUFFIX}@example.test",
                    "phone": "0712345678", "address": "Iasi", "coordinates": "47.17,27.58"}
        status, headers, data = http("POST", "/api/customers", customer)
        expect(201, status, "customer CREATE")
        created["customer"] = data["id"]
        assert headers["Location"].endswith("/api/customers/" + str(data["id"]))

        status, _, listing = http("GET", "/api/customers")
        expect(200, status, "customer LIST")
        assert any(item["id"] == created["customer"] for item in listing)
        status, headers, _ = http("GET", f'/api/customers/{created["customer"]}')
        expect(200, status, "customer READ")
        status, _, _ = http("GET", f'/api/customers/{created["customer"]}',
                            headers={"If-None-Match": headers["ETag"]})
        expect(304, status, "customer conditional GET")

        customer["name"] += " Updated"
        status, _, _ = http("PUT", f'/api/customers/{created["customer"]}', customer)
        expect(200, status, "customer UPDATE")

        vehicle = {"name": "Lab5 Van " + SUFFIX, "capacity": 100, "available": True}
        status, _, data = http("POST", "/api/vehicles", vehicle)
        expect(201, status, "vehicle CREATE (one depot, no driver)")
        created["vehicle"] = data["id"]
        assert data["depotId"] is not None

        status, _, listing = http("GET", "/api/vehicles")
        expect(200, status, "vehicle LIST")
        assert any(item["id"] == created["vehicle"] for item in listing)
        vehicle["capacity"] = 120
        status, _, data = http("PUT", f'/api/vehicles/{created["vehicle"]}', vehicle)
        expect(200, status, "vehicle UPDATE")
        assert data["capacity"] == 120
        status, _, _ = http("POST", "/api/vehicles", dict(vehicle, capacity=-5))
        expect(400, status, "Bean Validation rejects negative capacity")

        delivery = {"customerId": created["customer"], "quantity": 12, "notes": "Books"}
        status, _, data = http("POST", "/api/deliveries", delivery)
        expect(201, status, "delivery CREATE")
        created["delivery"] = data["id"]
        original_time = data["registeredAt"]
        status, _, listing = http("GET", "/api/deliveries")
        expect(200, status, "delivery LIST")
        assert any(item["id"] == created["delivery"] for item in listing)
        delivery["quantity"] = 20
        status, _, data = http("PUT", f'/api/deliveries/{created["delivery"]}', delivery)
        expect(200, status, "delivery UPDATE")
        assert data["registeredAt"] == original_time

        scenario = {"name": "Lab5 Scenario " + SUFFIX,
                    "vehicleIds": [created["vehicle"]], "deliveryIds": [created["delivery"]]}
        status, _, data = http("POST", "/api/scenarios", scenario)
        expect(201, status, "scenario CREATE / many-to-many links")
        created["scenario"] = data["id"]
        assert data["vehicleIds"] == scenario["vehicleIds"]
        assert data["deliveryIds"] == scenario["deliveryIds"]

        # A second independent scenario reuses the SAME vehicle and delivery.
        # This is the actual many-to-many property, not just one linked row.
        shared = dict(scenario, name="Shared Scenario " + SUFFIX)
        status, _, data = http("POST", "/api/scenarios", shared)
        expect(201, status, "second scenario shares vehicle and delivery (many-to-many)")
        created["scenario2"] = data["id"]
        assert data["vehicleIds"] == scenario["vehicleIds"]
        assert data["deliveryIds"] == scenario["deliveryIds"]

        status, _, duplicate = http("POST", "/api/scenarios", shared)
        expect(409, status, "unique scenario name enforced with HTTP 409")

        # Membership prevents an accidental DELETE of an in-use vehicle.
        status, _, _ = http("DELETE", f'/api/vehicles/{created["vehicle"]}')
        expect(409, status, "linked vehicle protected by foreign key")

        status, _, listing = http("GET", "/api/scenarios")
        expect(200, status, "scenario LIST")
        assert any(item["id"] == created["scenario"] for item in listing)

        path = f'/api/scenarios/{created["scenario"]}'
        status, headers, data = http("GET", path)
        expect(200, status, "scenario GET JSON")
        etag = headers["ETag"]
        status, _, data = http("GET", path, headers={"If-None-Match": etag})
        expect(304, status, "scenario GET If-None-Match")

        status, headers, xml = http("GET", path, headers={"Accept": "application/xml"})
        expect(200, status, "scenario GET XML content negotiation")
        assert "xml" in headers["Content-Type"].lower()
        ET.fromstring(xml)  # actual XML; not merely JSON with the wrong Content-Type

        scenario["name"] += " Updated"
        status, _, data = http("PUT", path, scenario)
        expect(200, status, "scenario UPDATE replaces name and membership")
        assert data["name"] == scenario["name"]
        status, headers, _ = http("GET", path, headers={"If-None-Match": etag})
        expect(200, status, "scenario changed -> old ETag no longer matches")
        assert headers["ETag"] != etag

        status, _, _ = http("POST", "/api/scenarios",
                            {"name": "Invalid " + SUFFIX, "vehicleIds": [999999999],
                             "deliveryIds": []})
        expect(400, status, "missing scenario member ID rejected")
        status, _, _ = http("GET", "/api/scenarios/999999999")
        expect(404, status, "custom ResourceNotFoundException maps to 404")
        status, _, spec = http("GET", "/v3/api-docs")
        expect(200, status, "OpenAPI endpoint")
        assert "/api/scenarios/{id}" in spec["paths"]

    finally:
        # Scenario first: links block deletion of referenced vehicles/deliveries.
        for entity in ("scenario2", "scenario", "delivery", "vehicle", "customer"):
            if entity in created:
                resource = "scenario" if entity == "scenario2" else entity
                path = f'/api/{resource}s/{created[entity]}'
                if entity == "delivery":
                    path = f'/api/deliveries/{created[entity]}'
                try:
                    status, _, _ = http("DELETE", path)
                    expect(204, status, f"{entity} DELETE (cleanup)")
                except Exception as error:
                    print("CLEANUP WARNING", entity, error)
    print("\nLAB 5 HTTP SMOKE TESTS PASSED")


if __name__ == "__main__":
    main()
