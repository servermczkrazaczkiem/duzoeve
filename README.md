# Eventowki 2.0.0 — 16 przedmiotów dla Paper 26.1.2

## Status — przeczytaj przed instalacją
To projekt źródłowy do zbudowania przez GitHub Actions, NIE gotowy plik JAR.
Nie wykonano kompilacji Java/Maven ani testu serwerowego w środowisku autora odpowiedzi.
Wykonano jedynie kontrole struktury projektu. Zielony Actions potwierdzi kompilację,
ale nie poprawność wszystkich interakcji w grze. Najpierw użyj osobnego serwera testowego i dwóch graczy.
Nie używaj na wartościowym ekwipunku, dopóki nie przejdziesz TESTY.md.

## Budowanie bez instalowania programów na komputerze
1. Rozpakuj ZIP. Otwórz folder Eventowki-projekt.
2. Wgraj jego ZAWARTOŚĆ do głównego katalogu nowego repozytorium GitHub.
   pom.xml i src muszą być na głównym poziomie. NIE wgrywaj samego ZIP-a.
3. Włącz pokazywanie ukrytych elementów i uwzględnij folder .github.
4. Jeśli folder .github nie przeszedł: Add file > Create new file, nazwa:
   .github/workflows/build.yml
   Wklej treść z BUDOWANIE.txt (sam kod YAML), zapisz Commit changes.
5. Actions > najnowszy Build Eventowki. Poczekaj na zielony wynik.
6. Otwórz wynik > Artifacts > Eventowki-plugin. Rozpakuj artefakt.
   Gotowy plik: Eventowki-2.0.0.jar.
7. Przy błędzie otwórz krok Zbuduj plugin i skopiuj końcowe wiersze [ERROR].

## Instalacja / migracja
1. Zrób kopię świata, plików graczy i katalogu plugins.
2. Zatrzymaj serwer.
3. Usuń stare JAR-y EventItems / Eventowki. Nie pozostawiaj dwóch wersji.
   Usuń/wyłącz też osobne stare pluginy tych samych przedmiotów (np. Granat, Rózga,
   Lewitacja), jeżeli zastępujesz je tym pakietem. Nie usuwaj innych pluginów.
4. Wgraj nowy JAR do plugins. Uruchom Paper 26.1.2 z Java 25.
5. Wydaj nowe przedmioty. Stare przedmioty z osobnych pluginów nie są migrowane.
6. Nie używaj /reload. Po zmianach konfiguracji wykonuj pełny restart.

## Komendy
W grze jako OP: /eventitems all
Konsola: eventitems all TWOJ_NICK
Lista: /eventitems lista
Pojedynczy przedmiot: /eventitems lasso
Dla gracza: /eventitems czaszka TWOJ_NICK 3
Uprawnienie: eventitems.admin (domyślnie OP). Alias: /eventowki.
Gdy ekwipunek jest pełny, wydawane komendą przedmioty spadają obok odbiorcy.
Torby wydawane są jako osobne egzemplarze, każda z własnym identyfikatorem.

## Przedmioty i identyfikatory
| ID | Nazwa | Mechanika |
|---|---|---|
| czaszka | Złota czaszka | Czaszka szkieleta w ekwipunku/offhand. Gdy śmierć powodowałaby utratę przedmiotów, zużywa jedną i zachowuje ekwipunek, ale nie XP. Jeśli keepInventory już działa, nie zużywa czaszki. |
| milosc | Miłość na zabój | PPM: pełne leczenie i ustawienie 9 złotych serc (nie sumuje). Bez limitu czasu, do zbicia/śmierci. CD 30 s. |
| piernik | Piernik | Ciastko. PPM: regeneracja III na 4 s. Zużywa 1. CD 10 s. |
| uszy | Uszy królika | Trafienie z obrażeniami: obrót celu o 180 stopni i blokada skoku na 5 s. Zużywa 1. CD 30 s. |
| laska | Laska cukrowa | Trzcina w głównej/offhand: +4 maksymalne czerwone serca. Dwie sztuki nie sumują bonusu. Nie leczy automatycznie. |
| banan | Skórka banana | Trafienie z obrażeniami: losowo miesza sloty 1–9 celu. Zużywa 1. CD 30 s. |
| rozga | Rózga 1 | Patyk. Trafienie nieszybującego gracza: elytra zablokowana 5 s, dźwięk rozbijanej potki. Brak zużycia. CD 30 s. Szybujący cel nie aktywuje efektu/CD. |
| klata | Gwiezdna klata | Kolczasta klata, ochrona V, niezniszczalna. W slocie klaty odnawia siłę I i szybkość I na 1 s co 0,25 s. Efekty po zdjęciu wygasają w maks. 1 s. |
| maczuga | Maczuga | Drewniany miecz. PPM: śnieżka. Trafiony gracz stoi w miejscu 3 s, może obracać kamerą. Cel w pojeździe jest pomijany. CD 30 s. |
| jablko | Smocze jabłko | Normalna animacja jedzenia, nie zużywa się. Głód i regeneracja jak zwykłe złote jabłko, 2 złote serca na 2 min; 10% szans na łącznie 4 zamiast 2. Nie sumuje złotych serc. Brak dodatkowego CD. |
| dynia | Granat dyniowy | Wygląd dyni. PPM rzuca pocisk mikstury. Wybuch do 5 kratek: dynia na głowę na 5 s również dla rzucającego. Zużywa 1. CD 30 s. |
| hak | Hak | Wędka. PPM natychmiast nadaje prędkość w kierunku rzutu, bez konieczności trafienia bloku. Kosmetyczny pocisk pokazuje kierunek. CD 30 s. |
| torba | Torba kangura | Łapa królika. PPM: 27 miejsc. Automatycznie zbiera wybrane łupy gracza zabitego przez właściciela. |
| lasso | Lasso | Smycz. PPM na gracza do 30 kratek: więź 5 s; odległość >=10 kratek teleportuje cel do właściciela. Na końcu 5 s ostatni teleport niezależnie od odległości. Drugi PPM wcześniej: ostatni teleport i natychmiastowy koniec, bez kolejnego po czasie. Elytry obu zablokowane 5 s. CD 30 s od pierwszego użycia. |
| rozdzka | Różdżka wiedźmy | End rod. PPM lewitacja użytkownika i graczy w promieniu 10 kratek; LPM lub trafienie: tylko wskazany cel. Lewitacja I na 5 s. CD 30 s. |
| granat | Granat odepchnięcia | Niezużywalna rzucana potka slow falling, bez nakładania slow falling. Wybuch odrzuca przeciwników do 6 kratek; bliżej = mocniej. Nie odrzuca rzucającego. CD 30 s z nakładką na przedmiocie. |

## Odnowienia
Serwer sprawdza cooldown per UUID gracza i rodzaj przedmiotu. Inna sztuka, slot lub ręka go nie omijają.
Znaczniki końca cooldownów zapisują się w data.yml i są odtwarzane po wejściu/restartach.
Nakładki używają osobnych grup: Uszy nie blokują Torby, a oba granaty nie blokują się nawzajem.
Przedmioty aktywujesz główną ręką; Laska działa w obu. Jabłko można jeść też z offhand.
Po zużyciu ostatniej sztuki nakładka nie jest widoczna, dopóki nie masz kolejnej sztuki.
Lasso: podczas aktywnych 5 s jest tekstowy licznik. Nakładka zaczyna się po końcu więzi
z pozostałym czasem do 30 s od aktywacji. To świadomy kompromis dla dostępności drugiego PPM.

## Torba — bezpieczeństwo danych
Automatyczne zbieranie: eventówki z TEGO pakietu, hełmy/klaty/spodnie/buty, miecze,
siekiery i elytry. Nie zbiera automatycznie zwykłych jabłek, bloków ani fajerwerków.
Nadmiar zostaje na ziemi. Przy Złotej czaszce/keepInventory nie kopiuje ekwipunku do torby.
Torba musi być w ekwipunku zabójcy, nie musi być trzymana. Używana jest pierwsza znaleziona.
W GUI klik stosu przenosi cały stos, nie jedną sztukę; klik przedmiotu w dolnym
własnym ekwipunku wkłada go ręcznie do torby. Brak wkładania torby do torby.
Zawartość jest zapisywana na serwerze w plugins/Eventowki/data.yml, a nie w lore.
Torba nie jest przenośna pomiędzy różnymi serwerami bez przeniesienia danych.
Kopie tej samej torby (np. klonowanie przez admina) współdzielą jeden magazyn.
NIE kasuj data.yml. Rób kopie tego pliku razem z danymi graczy.
Nagła awaria procesu/dysku nadal może spowodować niespójność zapisu; brak gwarancji antyduplikacyjnej
przy awarii systemu. Nie deklarujemy audytu bezpieczeństwa.

## Dynia
Hełm jest przenoszony do normalnego ekwipunku. Przy pełnym ekwipunku i założonym hełmie
cel jest pomijany, aby nie niszczyć ani nie wyrzucać hełmu. Kolejny wybuch odświeża 5 s.
Tymczasowej dyni nie można przenieść kliknięciem do torby ani wyrzucić.
Po końcu efektu poprzedni hełm wraca, jeśli gracz ma go nadal w ekwipunku i głowa jest wolna.
Po śmierci hełm podlega normalnym regułom śmierci/czaszki; nie jest magicznie odtwarzany.
Wyjście/restart kończy dynię wcześniej i sprząta stan przy kolejnym wejściu.

## Siła granatu
Nowy mocny preset: pozioma 3.0, pionowa 0.8. Zmienisz go w config.yml.
Nie mamy kodu/starych wartości użytego wcześniej pluginu, więc nie możemy potwierdzić
rzeczywistego 2x dystansu względem niego. Prędkość nie przekłada się liniowo na dystans.
Sprawdź na tej samej arenie i dopasuj horizontal-strength. Nie zmieniaj zasięgu, jeśli
chcesz porównywać samą odległość odrzutu. Kolizje, opór, lag i antycheat wpływają na wynik.

## Ograniczenia
Brak integracji z WorldGuardem, drużynami, antycheatem i systemem walki.
Efekty PPM/obszarowe/projektylowe nie sprawdzają flag PvP regionu; testuj poza spawnem.
Efekty trafienia wręcz pomijają anulowane lub zerowe obrażenia.
Inny plugin może anulować teleport, nadpisać efekt lub modyfikować ekwipunek przy śmierci.
Unieruchomienie jest kotwicą lokalizacji, nie ochroną przed obrażeniami; teleport kończy je.
Blokada skoku nie blokuje pływania, wspinania ani zewnętrznego wyrzucenia w górę.
Brak ochrony przed upadkiem po lewitacji/Haku. Nie stosuj w świecie z wartościowym ekwipunkiem bez testu.
Miłość nie używa czasowego efektu absorpcji, ale inne pluginy/jabłka/mleko mogą oddziaływać
z pulą absorpcji; to wspólna pula gry, nie osobne paski. Sprawdź łączenie w TESTY.md.
Złota czaszka nie chroni XP ani przed ingerencją innych pluginów/cudzych reguł klątw.
Nie ma migracji przedmiotów ze starych pluginów; wydaj nowe egzemplarze.
