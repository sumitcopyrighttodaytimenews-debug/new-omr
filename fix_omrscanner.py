import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

content = content.replace("val answers = mutableListOf<Int>()\n\n        for (q in 0 until numQuestions) {",
                          "val answers = mutableListOf<Int>()\n        val allOptionCoords = mutableListOf<List<Pair<Float, Float>>>()\n\n        for (q in 0 until numQuestions) {")

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(content)
