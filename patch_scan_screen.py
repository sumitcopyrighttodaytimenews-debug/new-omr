import re

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    content = f.read()

target1 = """                            OmrScanner.scan(softwareBitmap, numQuestions, numOptions)"""
replacement1 = """                            val answerKeyList = key?.answers ?: emptyList()
                            OmrScanner.scan(softwareBitmap, numQuestions, numOptions, answerKeyList)"""
content = content.replace(target1, replacement1)

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(content)
print("Updated ScanOmrScreen.kt scan call")
