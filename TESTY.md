# Testy odbiorowe — wykonaj na KOPII serwera

Wymagane: Paper 26.1.2, Java 25, dwóch graczy A/B, Survival, PvP włączone, kopia zapasowa.
Actions nie zastępuje tych testów. Wszystkie pozycje poniżej są DO WYKONANIA, nie oznaczają testów zaliczonych.

- [ ] Build: GitHub Actions mvn verify kończy się zielono i tworzy JAR.
- [ ] Start: brak błędów, /plugins pokazuje Eventowki, /eventitems lista pokazuje 16 pozycji.
- [ ] Uprawnienia: gracz bez OP/uprawnienia nie dostaje przedmiotów komendą.
- [ ] Czaszka: śmierć z jedną sztuką zachowuje EQ i usuwa czaszkę, nic nie duplikuje na ziemi.
- [ ] Czaszka: stos dwóch zużywa jedną; offhand działa. Zwykła czaszka nie działa.
- [ ] Bez czaszki: śmierć normalnie wyrzuca EQ. Z keepInventory czaszka nie zużywa się.
- [ ] Miłość: leczy, daje dokładnie 9 złotych serc, nie dokłada po kolejnej aktywacji.
- [ ] Miłość: po >30 min pozostałe serca nadal są; po śmierci znikają. Reconnect i restart nie odnawiają serc.
- [ ] Piernik: ubywa 1, regeneracja III 4 s, cooldown 10 s na drugą sztukę.
- [ ] Uszy: ubywa 1, cel obraca się o 180 stopni, nie skacze 5 s, może chodzić.
- [ ] Laska: jedna lub dwie w rękach dają tylko +4 serca. Zmiana slotu, zgon i relog nie kumulują bonusów.
- [ ] Banan: zmienia tylko sloty 1–9, zachowuje każdy stos i ilość, ubywa 1 banana.
- [ ] Rózga: nie startuje elytra przez 5 s. Cel już szybujący nie wyzwala efektu/CD.
- [ ] Klata: ochrona V, nie niszczy się, siła/szybkość I podczas noszenia; zdjęcie kończy efekty po <=1 s.
- [ ] Maczuga: śnieżka nie zadaje obrażeń, blokuje ruch 3 s, kamera się obraca; potem ruch wraca.
- [ ] Jabłko: pełna animacja jedzenia, liczba pozostaje stała, zwykle 2 serca, czasami 4 (wiele prób).
- [ ] Miłość + zwykłe jabłko + Smocze jabłko + mleko: sprawdź brak odnawiania/niezamierzonego sumowania absorpcji.
- [ ] Dynia: test blisko/daleko od wybuchu, granica 5 kratek, rzucający też trafiany, ubywa 1 sztuka.
- [ ] Dynia: hełm wraca po 5 s; pełne EQ nie gubi hełmu; powtórny wybuch odświeża czas.
- [ ] Dynia: śmierć z czaszką/bez czaszki, rozłączenie i restart nie kopiują hełmu/dyni.
- [ ] Hak: skok w kierunku spojrzenia, także w górę/w dół, cooldown, brak zwykłego łowienia.
- [ ] Torba: 27 slotów, wkładanie i wyjmowanie, shift i numery klawiatury nie kopiują rzeczy.
- [ ] Torba: po zabójstwie wybrane łupy trafiają do torby, zwykłe bloki/jabłka/fajerwerki zostają.
- [ ] Torba pełna: nadmiar na ziemi. Czaszka/keepInventory zabitego: brak kopii łupu.
- [ ] Torba: relog/restart zachowują zawartość; przeniesienie torby do B przenosi dostęp.
- [ ] Torba: jednoczesny dostęp do klonów przez admina odświeża oba widoki i nie kopiuje łupu.
- [ ] Lasso: odległość >=10 kratek daje teleport, możliwe kilka razy w 5 s.
- [ ] Lasso: na końcu 5 s teleport nawet przy odległości 2 kratek.
- [ ] Lasso: drugi PPM w 2 s teleportuje i kończy; w 5 s nie ma dodatkowego teleportu.
- [ ] Lasso: śmierć, wyjście, zmiana świata kończą więź. Cooldown nie znika.
- [ ] Różdżka: PPM użytkownik+obszar 10, LPM tylko cel, czas 5 s, cooldown wspólny dla obu trybów.
- [ ] Granat: nie ubywa, nie nakłada slow falling, odpycha innych nie rzucającego, CD 30 s widoczny.
- [ ] Granat: porównaj odrzut ze starym pluginem w tych samych warunkach i dopasuj config.
- [ ] Osobne grupy CD: Uszy vs Torba oraz Granat vs Dynia nie blokują się wzajemnie.
- [ ] Wszystkie CD: dodatkowy egzemplarz, zmiana slotu, ręki, relog/restart nie obchodzą czasu.
- [ ] Strefy chronione/antycheat: sprawdź zachowanie przed dopuszczeniem do produkcji.
