# Lokalno pokretanje i provera

## Moji zahtevi i pregled A-1

Posle prijave građanina navigacija sadrži „Moji zahtevi“ (`/moji-zahtevi`).
Lista koristi postojeću backend rutu `GET /api/a1/zahtevi/listaZahteva`;
pojedinačni prikaz koristi `GET /api/a1/zahtevi/{documentId}`. Vlasnik se ne šalje.
Obe frontend stranice koriste postojeći guard za ulogu GRADJANIN.

Za ručnu proveru:

1. Pokrenite backend i frontend sa proxy-jem prema koracima ispod.
2. Prijavite građanina i otvorite „Moji zahtevi“. Bez njegovih dokumenata
   prikazuje se prazna lista sa porukom. Sa dokumentima se prikazuju naslov i vrsta.
3. Ako nemate dokumente, kreirajte ih kroz postojeći Postman POST
   `/api/a1/zahtevi`, koristeći građaninov token i `a1-zahtev-primer.xml`.
   Backend dodeljuje ID i vlasnika. Frontend još nema obrazac za kreiranje.
4. Kliknite „Pregled zahteva“. Proverite podnosioca, kontakt, delo, autorstvo,
   potpis i prisutne opcione podatke. Probajte fizičko i pravno lice, anonimno
   autorstvo, podnosioca kao autora i listu živih/preminulih autora.
5. U istoj kartici prijavite drugog građanina: ne sme videti tuđe zahteve.
   Direktni URL `/moji-zahtevi/{tuđiDocumentId}` prikazuje backend 404 poruku.
   Nepostojeći bezbedan ID daje isti rezultat. Podaci ranijeg zahteva se brišu
   pri promeni URL-a, pre učitavanja novog.
6. Bez prijave obe stranice vode na prijavu. Službenik nema građansku stavku
   navigacije; guard ga sa građanskih stranica vraća na početnu.
7. Zaustavite bazu uz pokrenut backend: greška 503 se prikazuje kao poruka,
   a ne kao prazna lista. U Network proverite Accept: application/xml,
   tekstualni XML odgovor i postojeće Bearer zaglavlje, bez vlasnikId parametra.

Prikaz koristi Angular tekstualne interpolacije, bez innerHTML. Reference priloga
i potpisa su tekst, bez učitavanja resursa. Neispravan XML/namespace daje poruku
o neispravnom odgovoru servera. Stari dokument bez ID-a može biti na listi, ali
bez dugmeta za pregled jer lista ne sadrži naziv njegovog fajla u bazi.

Pokrenite eXist-db na portu 8081 i backend iz Eclipse-a na portu 8080.
Backend Run Configuration mora imati `EXIST_DB_PASSWORD`. Za službenika pratite
`../xml-backend/SLUZBENIK.md`.

Iz korena repozitorijuma:

```powershell
cd xml-frontend
npm start
```

Otvorite `http://localhost:4200`. Proxy je uključen kroz `angular.json`.
Eksplicitna ekvivalentna komanda je:

```powershell
npm start -- --proxy-config proxy.conf.json
```

`/api` se prosleđuje na `http://localhost:8080`. Proxy važi za razvojni server;
produkcija zahteva zasebno prosleđivanje `/api` na backend. Nema novih paketa.

## Ručna provera

1. Otvorite Registraciju. Unesite izmišljeno ime, prezime, jedinstven email i
   lozinku od najmanje osam znakova, najviše 72 UTF-8 bajta. Probajte posebne
   znakove (`&`, `<`) i početne/završne razmake u lozinci. Lozinka se ne menja.
2. Uspešan odgovor 201 vodi na Prijavu sa porukom o uspešnoj registraciji.
   Ponovljena registracija istog emaila prikazuje XML poruku greške (409).
3. Pogrešna lozinka prikazuje poruku backend-a (401). Ispravna prijava vodi na
   početnu stranicu sa ulogom Građanin. Prijava predefinisanog službenika
   prikazuje ulogu Službenik. Registracija službenika ne postoji.
4. U DevTools → Network proverite XML tela, namespace i zaglavlja `Content-Type`
   i `Accept`. Prijava vraća `token` i `expiresAt`; registracija nema telo odgovora.
5. Osvežite početnu stranicu: prijava ostaje u istoj kartici preko sessionStorage.
   Odjava uklanja token i expiresAt i vodi na Prijavu. Otvaranje `/` bez prijave
   takođe vodi na Prijavu.
6. U DevTools → Application → Session Storage postavite `expiresAt` u prošlost
   i osvežite stranicu: sesija se briše i traži nova prijava.

Lozinka se ne čuva u storage-u. JWT claim `uloga` služi prikazu i guard-u;
backend proverava potpis i stvarna prava. Važenje je 24 sata, bez refresh tokena.
Interceptor šalje Bearer token samo našem `/api` i izuzima registraciju/prijavu.
XML poruke greške prikazuju se kao tekst, bez ubacivanja HTML-a.

## Automatske provere

```powershell
npm run build
npm test -- --watch=false --browsers=ChromeHeadless
```

Ako nema Chrome-a, postojeći launcher može koristiti instalirani Edge:

```powershell
$env:CHROME_BIN = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
npm test -- --watch=false --browsers=ChromeHeadless
```

Testovi koriste HTTP stub, bez stvarne baze. Pokrivaju XML escaping, očuvanje
lozinke, odgovor prijave, uloge, istek, interceptor, guard i odjavu.
