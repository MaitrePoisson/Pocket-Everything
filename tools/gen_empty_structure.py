import gzip
import struct
import sys

def s(v):
    b = v.encode("utf-8")
    return struct.pack(">H", len(b)) + b

def tag(tid, name, payload):
    return bytes([tid]) + s(name) + payload

def tint(name, v):
    return tag(3, name, struct.pack(">i", v))

def tstr(name, v):
    return tag(8, name, s(v))

def tlist(name, elem_id, items):
    return tag(9, name, bytes([elem_id]) + struct.pack(">i", len(items)) + b"".join(items))

def intlist(values):
    return b"".join(struct.pack(">i", v) for v in values)

def compound_body(parts):
    return b"".join(parts) + b"\x00"

W, H, D = 8, 4, 8
blocks = []
for x in range(W):
    for z in range(D):
        blocks.append(compound_body([
            tint("state", 0),
            tag(9, "pos", bytes([3]) + struct.pack(">i", 3) + intlist([x, 0, z])),
        ]))
palette = [compound_body([tstr("Name", "minecraft:stone")])]
root = compound_body([
    tint("DataVersion", 3955),
    tag(9, "size", bytes([3]) + struct.pack(">i", 3) + intlist([W, H, D])),
    tlist("palette", 10, palette),
    tlist("blocks", 10, blocks),
    tlist("entities", 10, []),
])
data = bytes([10]) + s("") + root
out = sys.argv[1]
with open(out, "wb") as f:
    f.write(gzip.compress(data, mtime=0))
