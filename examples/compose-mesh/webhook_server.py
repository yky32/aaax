#!/usr/bin/env python3
"""Webhook sink stub — verifies X-AAAX-Signature (HMAC-SHA256)."""
import hashlib
import hmac
import json
import os
from http.server import BaseHTTPRequestHandler, HTTPServer


SECRET = os.environ.get("AAAX_EVENTS_WEBHOOK_SECRET", "mesh-demo-secret").encode()
PORT = int(os.environ.get("PORT", "8099"))


class Handler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):  # noqa: A003 — stdlib signature
        print("[webhook] " + (format % args), flush=True)

    def do_POST(self):
        length = int(self.headers.get("content-length", 0))
        body = self.rfile.read(length)
        sig = self.headers.get("x-aaax-signature", "")
        event_id = self.headers.get("x-aaax-event-id") or self.headers.get("ce-id")
        expected = "sha256=" + hmac.new(SECRET, body, hashlib.sha256).hexdigest()
        ok = hmac.compare_digest(sig, expected)
        t = "?"
        evt = {}
        try:
            evt = json.loads(body.decode("utf-8"))
            t = evt.get("type", "?")
        except Exception:
            pass
        if not ok:
            print(f"[webhook] BAD SIG type={t} id={event_id}", flush=True)
            self.send_response(401)
            self.end_headers()
            self.wfile.write(b'{"ok":false,"error":"bad signature"}')
            return
        print(
            f"[webhook] OK type={t} id={event_id} delivery={self.headers.get('x-aaax-delivery-id')}",
            flush=True,
        )
        if t == "com.aaax.otp.dispatch":
            data = evt.get("data") or {}
            print(f"  → would notify dest={data.get('destination')} code={data.get('code')}", flush=True)
        self.send_response(204)
        self.end_headers()


if __name__ == "__main__":
    print(f"[webhook] listening :{PORT} (HMAC secret set)", flush=True)
    HTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
