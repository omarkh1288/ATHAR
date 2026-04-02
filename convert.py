import os, re

dirs_to_update = [
    r"c:\Users\Omar\OneDrive\Desktop\android\app\src\main\java\com\athar\accessibilitymapping\ui\components",
    r"c:\Users\Omar\OneDrive\Desktop\android\app\src\main\java\com\athar\accessibilitymapping\ui\payment"
]
files_to_update = [
    r"c:\Users\Omar\OneDrive\Desktop\android\app\src\main\java\com\athar\accessibilitymapping\ui\screens\SimpleCameraTranslator.kt",
    r"c:\Users\Omar\OneDrive\Desktop\android\app\src\main\java\com\athar\accessibilitymapping\ui\screens\SignLanguageRecognition.kt",
    r"c:\Users\Omar\OneDrive\Desktop\android\app\src\main\java\com\athar\accessibilitymapping\ui\screens\SignLanguageTranslatorScreen.kt"
]

all_files = []
for d in dirs_to_update:
    for root, _, files in os.walk(d):
        for f in files:
            if f.endswith('.kt'):
                all_files.append(os.path.join(root, f))
all_files.extend(files_to_update)

for fpath in all_files:
    if not os.path.exists(fpath): continue
    with open(fpath, 'r', encoding='utf-8') as f:
        content = f.read()

    new_content = re.sub(r'(\b\d+(?:\.\d+f?)?)\.dp\b', r'\1.sdp', content)
    new_content = re.sub(r'(\b\d+(?:\.\d+f?)?)\.sp\b', r'\1.ssp', new_content)
    
    if content != new_content:
        # Check if imports already there
        if 'import com.athar.accessibilitymapping.ui.theme.sdp' not in new_content:
            new_content = re.sub(r'(package\s+[^\n]+)', r'\1\n\nimport com.athar.accessibilitymapping.ui.theme.sdp', new_content)
        if 'import com.athar.accessibilitymapping.ui.theme.ssp' not in new_content:
            new_content = re.sub(r'(package\s+[^\n]+)', r'\1\nimport com.athar.accessibilitymapping.ui.theme.ssp', new_content)
            
        with open(fpath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {fpath}")
