import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

# Replace the mapVirtualToActual block
pattern = re.compile(r"fun mapVirtualToActual\(vx: Float, vy: Float\): Pair<Float, Float> \{.*?\n        \}", re.DOTALL)
match = pattern.search(content)

if match:
    new_func = """fun mapVirtualToActual(vx: Float, vy: Float): Pair<Float, Float> {
            val pts = floatArrayOf(vx, vy)
            matrix.mapPoints(pts)
            return Pair(pts[0], pts[1])
        }"""
    content = content[:match.start()] + new_func + content[match.end():]
    with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
        f.write(content)
    print("Replaced mapVirtualToActual!")
else:
    print("Pattern mapVirtualToActual not found!")
