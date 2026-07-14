import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    lines = f.readlines()

out = []
skip = False
for line in lines:
    if "val tl = findCorner" in line:
        skip = True
        out.append("        // Assume the bitmap is cropped by ML Kit Document Scanner\n")
        out.append("        val src = floatArrayOf(0f, 0f, width.toFloat(), 0f, width.toFloat(), height.toFloat(), 0f, height.toFloat())\n")
        out.append("        val dst = floatArrayOf(0f, 0f, 1000f, 0f, 1000f, 1414f, 0f, 1414f)\n")
        continue
    
    if skip:
        if "val matrix = Matrix()" in line:
            skip = False
            out.append(line)
        continue
    
    if "val searchRadius = Math.max(10, bitmap.width / 60)" in line:
        out.append(line.replace("Math.max(10, bitmap.width / 60)", "Math.max(25, bitmap.width / 20)"))
        continue
        
    out.append(line)

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.writelines(out)
