Ten server proxy przy filtrowaniu słów usuwa je z textu zamiast je oznaczać.
Parametry wejściowe:
PROXY_PORT - tak jak w opisie zadania np. PROXY_PORT=8080
WORDS - Różne wyrażenia powinny być oddzielane za pomocą znaku ';'. Natomiast spacje w wyrażeniu zastępuje znak '_'. np. WORDS=bomba;muchomor;atomowa;twarda_woda;darmowe_szkolenie
CACHE_DIR - tak jak w opisie zadania np. "C:\Users\tomur\Documents\cache"
Dodatkowe parametry:
USE_CACHE - czy używać cachowania. Domyślnie ustawione na true np. USE_CACHE=True
HEAVY_MODE - czy server powinien działać w trybie heavy. Domyślnie ustawione na true. np. HEAVY_MODE=False
Parametry powinny być od siebie odzielone spacjami. Przykład poprawnego podania parametrów:
PROXY_PORT=8080 WORDS=bomba;muchomor;atomowa;twarda_woda;darmowe_szkolenie CACHE_DIR="C:\Users\tomur\Documents\cache" USE_CACHE=True HEAVY_MODE=False


