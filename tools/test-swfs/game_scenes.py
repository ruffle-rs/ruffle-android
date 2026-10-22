"""Writes game_scenes.swf: a movie that keeps its scenes like the games Ruffle's
game scene menu is for. It has a `scenelist`, the current scene number in `pos`,
and a `setScreen(name)` function that shows a scene, tracing "showing <scene>"."""
import struct
import sys


def tag(code, body=b""):
    if len(body) < 63:
        return struct.pack("<H", code << 6 | len(body)) + body
    return struct.pack("<HI", code << 6 | 63, len(body)) + body


def action(code, body=b""):
    if code < 0x80:
        return bytes([code])
    return bytes([code]) + struct.pack("<H", len(body)) + body


def push(*values):
    body = b""
    for value in values:
        if isinstance(value, int):
            body += b"\x07" + struct.pack("<i", value)
        else:
            body += b"\x00" + value.encode() + b"\x00"
    return action(0x96, body)


def define_function(name, params, code):
    body = name.encode() + b"\0" + struct.pack("<H", len(params))
    body += b"".join(param.encode() + b"\0" for param in params)
    return action(0x9B, body + struct.pack("<H", len(code))) + code


GET_VARIABLE, SET_VARIABLE, DEFINE_LOCAL, DEFINE_LOCAL2 = 0x1C, 0x1D, 0x3C, 0x41
INIT_ARRAY, ADD2, TRACE, STOP = 0x42, 0x47, 0x26, 0x07
END = b"\x00"

# screen_name = name; trace("showing " + name);
show_scene = (push("screen_name", "name") + action(GET_VARIABLE) + action(SET_VARIABLE)
              + push("showing ", "name") + action(GET_VARIABLE) + action(ADD2) + action(TRACE))

# var scenelist = ["intro", "fight", "ending"]; var pos = 1; var screen_name;
# function setScreen(name) { ... } trace("showing intro"); stop();
code = (push("scenelist", "ending", "fight", "intro", 3) + action(INIT_ARRAY) + action(DEFINE_LOCAL)
        + push("pos", 1) + action(DEFINE_LOCAL) + push("screen_name") + action(DEFINE_LOCAL2)
        + define_function("setScreen", ["name"], show_scene)
        + push("showing intro") + action(TRACE) + action(STOP) + END)


def rect(xmin, xmax, ymin, ymax, bits=16):
    fields = format(bits, "05b") + "".join(format(v, f"0{bits}b") for v in (xmin, xmax, ymin, ymax))
    fields += "0" * (-len(fields) % 8)
    return int(fields, 2).to_bytes(len(fields) // 8, "big")


body = rect(0, 550 * 20, 0, 400 * 20) + struct.pack("<BBH", 0, 24, 1)
body += tag(69, struct.pack("<I", 0))  # FileAttributes
body += tag(9, b"\xff\xff\xff")  # SetBackgroundColor
body += tag(12, code) + tag(1) + tag(0)
sys.stdout.buffer.write(b"FWS" + bytes([8]) + struct.pack("<I", 8 + len(body)) + body)
