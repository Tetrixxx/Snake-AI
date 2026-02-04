# 🐍 Snake AI Agent

Intelligens AI ágens a klasszikus Snake játékhoz, amely A* útvonalkeresést és heurisztikus döntéshozatalt használ a maximális pontszám eléréséhez.

## 📋 Projekt leírása

Ez a projekt egy optimalizált AI ágensimplementáció Java nyelven, amely képes a Snake játékot játszani. Az ágens fejlett algoritmusokat használ az étel hatékony összegyűjtéséhez, miközben elkerüli az ütközéseket és maximalizálja a túlélési esélyeket.

### Főbb jellemzők

- **A\* útvonalkeresés**: Optimális út keresése az ételhez
- **Heurisztikus értékelés**: Intelligens döntéshozatal több tényező alapján
- **Biztonsági ellenőrzés**: Megelőző stratégia a csapdahelyzetek elkerülésére
- **Farokkövetés**: Adaptív stratégia amikor nincs biztonságos út az ételhez
- **Dinamikus súlyozás**: A kígyó hosszától függő paraméterhangoláss

## 🎮 Játék szabályok

- **Pálya méret**: 15×25 (15 sor, 25 oszlop)
- **Pontozás**: 
  - Kezdőérték: 0
  - Minden lépés: -1 pont
  - Étel felvétele: +100 pont
- **Játék vége**: Falnak vagy önmagának ütközés, időtúllépés, vagy túl hosszú tétlenség

## 🚀 Használat

### Előfeltételek

- Java SDK 8 vagy újabb

### Fordítás

```bash
javac -cp game_engine.jar Agent.java
```

### Futtatás vizualizációval (10 fps)

```bash
java -jar game_engine.jar 10 game.snake.SnakeGame 1234567890 15 25 10000 Agent
```

### Tesztelés vizualizáció nélkül

```bash
java -Xmx2G -jar game_engine.jar 0 game.snake.SnakeGame 1234567890 15 25 10000 Agent
```

### Paraméterek

- `10` - Megjelenítési sebesség (frames/sec, 0: nincs GUI)
- `game.snake.SnakeGame` - Játéklogika osztály
- `1234567890` - Random seed
- `15` - Pálya magassága
- `25` - Pálya szélessége
- `10000` - Rendelkezésre álló idő (ms)
- `Agent` - Az AI osztály neve

### Játék visszajátszása log fájlból

```bash
java -jar game_engine.jar 25 gameplay_xxxxxxxxx.data
```

## 🧠 Algoritmus áttekintés

Az ágens működése több rétegű döntéshozatalon alapul:

### 1. Étel kiválasztása
- **Távolság vs. tér értékelés**: A közeli ételek preferálása, de figyelembe véve a környező szabad területet
- **Adaptív súlyozás**: Rövid kígyó esetén a távolság számít, hosszú kígyónál a biztonság

### 2. Útvonaltervezés (A*)
- Optimális út keresése az A* algoritmussal
- Dinamikus büntetések:
  - Kígyó testének közelsége
  - Falak közelsége
  - Szűk helyek
- Heurisztika: Manhattan távolság

### 3. Biztonsági ellenőrzés
- **Elérhető tér számítás**: BFS-sel ellenőrzi, hogy van-e elég hely a lépés után
- **Minimum tér követelmény**: Legalább a kígyó hosszának megfelelő szabad tér szükséges
- **Farok elérhetőség**: Hosszú kígyó esetén biztosítja, hogy a farok elérhető maradjon

### 4. Farokkövetés
- Ha nincs biztonságos út az ételhez, a kígyó a saját farkát követi
- Biztosítja, hogy ne essen csapdába

### 5. Vészhelyzeti manőverek
- Ha minden más sikertelen, a legtöbb szabad területet eredményező irányt választja
- Falak és a test elkerülése prioritás

## 📊 Teljesítmény

- **Teljesítési követelmény**: Minimum 3000 pont 10 játékból legalább 8 alkalommal
- **Átlagos pontszám**: Az optimalizált V16 verzió következetesen eléri a célpontszámot

## 📁 Projekt struktúra

```
Snake-ai/
├── src/
│   ├── Agent.java           # Fő AI implementáció
│   ├── game_engine.jar      # Játékmotor
│   ├── SNAKE.md             # Feladat specifikáció
│   ├── SNAKE.pdf            # Feladat dokumentáció
│   └── _run.ps1             # PowerShell futtatószkript
└── README.md
```

## 🛠️ Fejlesztési megjegyzések

### Verziótörténet
- **V16**: Jelenlegi optimalizált verzió finomhangolt paraméterekkel
- Korábbi verziók: Különböző stratégiák tesztelése (Hamilton-út, Q-learning, stb.)

### Optimalizációk
- Gyorsított étel elérés rövid kígyó esetén
- Erősebb túlélési stratégia hosszú kígyó esetén
- Dinamikus súlyozás a kígyó hossza alapján
- Szűk helyek elkerülése hosszú kígyó esetén

## 📝 Követelmények

- A megoldás saját munka kell hogy legyen
- ASCII karakterkódolás (UTF-8 ajánlott)
- Nincs külső könyvtár használat (csak JDK)
- Nincs képernyőre írás, fájlműveletek vagy többszálúság
- Teljes magyar nyelvű dokumentáció

## 👨‍💻 Szerző

- **Neptun kód**: h265832
- **Email**: h265832@stud.u-szeged.hu
- **Nick**: McBuktam

## 📄 Licensz

Ez a projekt egy egyetemi kötelező program része a Szegedi Tudományegyetem Mesterséges Intelligencia kurzusához.

## 🤝 Közreműködés

Ez egy egyetemi projektmunka, így külső közreműködés nem lehetséges. A konzultáció és közös ötletelés megengedett volt a fejlesztés során, de a megvalósítás önálló munka.

---

*Utolsó frissítés: 2024. december*
