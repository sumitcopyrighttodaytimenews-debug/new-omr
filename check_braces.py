with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    content = f.read()

count = 0
for i, c in enumerate(content):
    if c == '{': count += 1
    elif c == '}': count -= 1
    if count < 0:
        print(f"Unmatched brace at {i}")
print(f"Final count: {count}")
