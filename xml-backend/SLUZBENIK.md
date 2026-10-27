# Predefinisani razvojni službenik

Koristi se postojeći `korisnik.xsd` i postojeći login. Nema startup mehanizma
niti endpointa za kreiranje ili izmenu službenika. Javna registracija i dalje
kreira samo `GRADJANIN`.

Razvojni kredencijali (samo za lokalno testiranje):

- E-mail: `sluzbenik.razvoj@example.org`
- Lozinka: `SluzbenikTest123!`
- Ime i prezime: Jelena Primerić
- URL ID: `https://example.org/autorska-prava/nalozi/58675d5b-2e64-439f-ae84-2b70741a0653`

`src/main/resources/sluzbenik-primer.xml` sadrži samo BCrypt hash, ne običnu
lozinku. E-mail je već normalizovan prema postojećem `EmailNormalizer` pravilu.

## Jednokratna priprema u lokalnom eXist-db-u

Sve korake izvršava administrator baze, van javnog API-ja aplikacije:

1. Ako korisnička kolekcija još nije pripremljena, izvrši postojeći
   `src/main/resources/exist/korisnici-init.xq` u eXide-u. Ne prepisuj lock
   dokument dok backend radi.
2. U eXide-u izvrši kratku naredbu:

   ```xquery
   xmldb:create-collection('/db/autorska-prava', 'priprema')
   ```

3. Kroz administratorski pregled kolekcija učitaj `sluzbenik-primer.xml` kao
   `/db/autorska-prava/priprema/sluzbenik-primer.xml`. Ovo je izvor za pripremu,
   ne nalog za login; nemoj ga ručno uploadovati u korisničku kolekciju.
4. U lokalnom eXide-u (`http://localhost:8081/exist/apps/eXide/`), kao `admin`,
   izvrši `src/main/resources/exist/sluzbenik-priprema.xq`.

Skripta koristi isto zaključavanje kao registracija. Proverava e-mail u
`/db/autorska-prava/korisnici`, pa čuva izvorni XML samo ako nalog ne postoji.
Ne ispisuje hash niti XML naloga.

- `<created/>`: razvojni službenik je upisan.
- `<alreadyPresent/>`: e-mail već pripada službeniku; postojeći nalog,
  uključujući njegov ID i hash, ostaje nepromenjen. Tada važe postojeći kredencijali,
  koji ne moraju biti identični gornjem primeru.
- `CONFLICT_EMAIL`: e-mail je zauzet drugim nalogom ili postoji više takvih
  naloga. Građaninu se ne menja uloga. Administrator treba da proveri konflikt.
- `CONFLICT_ACCOUNT`: ID ili odredišni dokument je zauzet; nema prepisivanja.
- `INVALID_PREPARATION`: proveri izvorni razvojni XML i postojeći lock dokument.

Ponovno izvršavanje ne resetuje lozinku i ne prepisuje postojeći nalog.

## Postman: login → lista → dokument

1. Pokreni backend u Eclipse-u sa podešenim `EXIST_DB_PASSWORD`.
2. `POST http://localhost:8080/api/auth/prijava`, **No Auth**,
   `Content-Type: application/xml; charset=UTF-8`, `Accept: application/xml`:

   ```xml
   <p:prijava xmlns:p="https://example.org/autorska-prava/prijava">
     <p:email>sluzbenik.razvoj@example.org</p:email>
     <p:lozinka>SluzbenikTest123!</p:lozinka>
   </p:prijava>
   ```

3. Očekuj 200. Kopiraj `token` iz XML odgovora (access token važi 24 sata).
4. `GET http://localhost:8080/api/sluzbenik/a1/zahtevi`, **Bearer Token**, uz
   `Accept: application/xml`. Očekuj 200 i validan `listaZahteva`, uključujući
   zahteve drugih naloga i stare dokumente bez `vlasnikId`.
5. Za zahtev iz liste otvori URL iz njegovog `id` preko GET-a sa istim tokenom.
   Postojeća pojedinačna ruta je `/api/a1/zahtevi/{documentId}`. Za stare razvojne
   dokumente bez `id` koristi poznati documentId iz naziva dokumenta u bazi.

Bez tokena lista vraća XML 401, sa građanskim tokenom XML 403.
Prazna ili nepostojeća A-1 kolekcija daje 200 i praznu listu.
Greške pristupa, konekcije ili XML odgovora baze daju XML 503.
Nema sortiranja, paginacije, pretrage ili filtriranja po vlasniku u službeničkom upitu.
Građanske rute ostaju nepromenjene, uključujući trenutnu listu
`/api/a1/zahtevi/listaZahteva`.

## Šta je provereno

Automatski testovi proveravaju razvojni XML prema `korisnik.xsd`, normalizaciju
e-maila i BCrypt hash, službenički login preko postojećeg endpointa i stvarni
security lanac, listu sa dokumentima različitih vlasnika i bez vlasnika,
praznu listu, 401/403/503 i pojedinačni GET. Repository test koristi lokalni HTTP
stub i proverava XQuery POST kroz `execute` i `queryBody`, bez owner filtera.

Pripremna skripta i upiti nisu izvršeni na stvarnoj bazi tokom ove implementacije.
Jednokratni upis, ponavljanje pripreme i konflikt sa postojećim građaninom
zahtevaju ručnu proveru u lokalnom eXist-db-u prema gornjim koracima.
