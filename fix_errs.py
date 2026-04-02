files = {
    r"c:\Users\Omar\OneDrive\Desktop\android\app\src\main\java\com\athar\accessibilitymapping\ui\components\ToggleSwitch.kt": [54],
    r"c:\Users\Omar\OneDrive\Desktop\android\app\src\main\java\com\athar\accessibilitymapping\ui\payment\AtharPaymentFlow.kt": [777],
    r"c:\Users\Omar\OneDrive\Desktop\android\app\src\main\java\com\athar\accessibilitymapping\ui\screens\SignLanguageTranslatorScreen.kt": [68,69,75,76,82,83,1431],
    r"c:\Users\Omar\OneDrive\Desktop\android\app\src\main\java\com\athar\accessibilitymapping\ui\screens\SimpleCameraTranslator.kt": [434,653,670,675,676]
}
for fpath, lines in files.items():
    with open(fpath, 'r', encoding='utf-8') as f:
        content = f.readlines()
    for l in lines:
        content[l-1] = content[l-1].replace('.sdp', '.dp').replace('.ssp', '.sp')
    with open(fpath, 'w', encoding='utf-8') as f:
        f.writelines(content)
print("Fixes applied.")
