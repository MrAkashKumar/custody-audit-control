"""Black-box HTTP smoke test for a running demo instance (creates a test proposal/report)."""
import http.cookiejar
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request

BASE = os.environ.get('CC_BASE_URL', 'http://127.0.0.1:8091')
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

def call(path, data=None, form=False, token=None):
    headers = {}
    if token:
        headers[token['headerName']] = token['token']
    if data is not None:
        headers['Content-Type'] = 'application/x-www-form-urlencoded' if form else 'application/json'
        data = (urllib.parse.urlencode(data) if form else json.dumps(data)).encode()
    request = urllib.request.Request(BASE + path, data=data, headers=headers)
    with client.open(request, timeout=20) as response:
        body = response.read()
        return json.loads(body) if 'json' in response.headers.get('Content-Type', '') else body

assert call('/')[:15].lower().startswith(b'<!doctype html>')
token = call('/api/v1/csrf')
call('/api/v1/login', {'username': 'maker', 'password': os.environ.get('CC_DEMO_PASSWORD', 'CustodyDemo!2026')}, form=True, token=token)
token = call('/api/v1/csrf')
assert call('/api/v1/session')['user'] == 'maker'
assert len(call('/api/v1/features')) == 6
result = call('/api/v1/audit?category=STATIC&size=20')
assert result['totalElements'] > 0
job = call('/api/v1/reports', {'format': 'CSV', 'filter': {'category': 'STATIC', 'feature': 'vault', 'scope': 'BULLION'}}, token=token)
for attempt in range(15):
    report = next(r for r in call('/api/v1/reports') if r['id'] == job['id'])
    if report['status'] == 'READY':
        break
    assert report['status'] != 'FAILED', report
    time.sleep(1)
assert b'Selection criteria' in call('/api/v1/reports/' + job['id'] + '/download')
contract = call('/v3/api-docs')
assert '/api/v1/reports/{id}/download' in contract['paths']
print('PASS: login + CSRF, six features, real audit search, queued CSV generation, secure download, OpenAPI')
