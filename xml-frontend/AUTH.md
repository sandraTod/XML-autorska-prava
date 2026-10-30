# Lokalno pokretanje i provera

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
