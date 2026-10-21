# Lokalna registracija građanina

Backend koristi Java 17. eXist-db ostaje zaseban proces na portu 8081.

## Jednokratna priprema baze

Pre pokretanja registracije, prijavi se kao administrator u lokalni eXide
(`http://localhost:8081/exist/apps/eXide/`) i izvrši sadržaj
`src/main/resources/exist/korisnici-init.xq`. Očekivan rezultat je `READY`
ili `ALREADY_READY`. Skripta kreira kolekciju `/db/autorska-prava/korisnici`
i dokument `registracija-lock.xml`. Nemoj ga brisati ili prepisivati dok aplikacija radi.
Skriptu izvrši jednom, pre slanja registracionih zahteva.

Bez tog dokumenta upis se odbija sa 503, umesto nezaštićenog upisa.
`util:exclusive-lock` u bazi obuhvata proveru normalizovanog e-maila i čuvanje,
pa se zaštita ne oslanja na Java `synchronized` ili na jednu backend instancu.
Korisnički XML i e-mail prenose se kao spoljne REST XQuery promenljive.
Tekst XQuery-a je statičan. Ne dodavati korisnike ručno mimo ovog pravila.

## Pokretanje backenda

Iz `xml-backend`, u PowerShell terminalu koji koristi Java 17:

```powershell
$credential = Get-Credential -UserName admin -Message 'eXist-db pristup'
$env:EXIST_DB_USERNAME = $credential.UserName
$env:EXIST_DB_PASSWORD = $credential.GetNetworkCredential().Password
./mvnw.cmd spring-boot:run
```

Lozinku nemoj upisivati u properties, Git ili komandni argument. Nakon zaustavljanja:

```powershell
Remove-Item Env:EXIST_DB_PASSWORD
```

Podrazumevani URL baze je `http://localhost:8081/exist/rest`; menja se kroz
`EXIST_DB_URL`. Osnova identifikatora naloga je
`https://example.org/autorska-prava/nalozi`; menja se kroz
`KORISNIK_DOCUMENT_BASE_URL`. To je identifikator, ne javni GET endpoint.

## Postman

- Method: `POST`
- URL: `http://localhost:8080/api/auth/registracija`
- Headers: `Content-Type: application/xml; charset=UTF-8`, `Accept: application/xml`
- Body: `raw`, XML:

```xml
<r:registracija xmlns:r="https://example.org/autorska-prava/registracija">
  <r:ime>Mila</r:ime>
  <r:prezime>Primerić</r:prezime>
  <r:email>mila.primer@example.org</r:email>
  <r:lozinka>RazvojTest123!</r:lozinka>
</r:registracija>
```

Očekuj 201 bez tela. Ponovljen zahtev, uključujući drugačiju veličinu slova
ili razmake oko e-maila, vraća 409. E-mail se normalizuje sa `strip()` i
`toLowerCase(Locale.ROOT)`; lozinka se ne menja.
Neispravan XML/e-mail ili lozinka kraća od 8 Unicode znakova odnosno duža od
72 UTF-8 bajta vraća 400. Greška baze ili nedostajuća inicijalizacija vraća 503.
Greške su XML, bez lozinke, hash-a ili detalja parsera/baze.
Službenik i klijentski ID nisu dozvoljeni u registracionom XML-u.

## Testovi

```powershell
./mvnw.cmd -B test
```

Ovi testovi koriste mock repository i lokalni HTTP stub; ne potvrđuju rad stvarne baze.
Za zasebnu proveru istovremenih upisa iz dva repository klijenta na stvarnoj bazi,
posle inicijalizacije i podešavanja environment kredencijala:

```powershell
$env:KORISNIK_DB_INTEGRATION_TEST = 'true'
./mvnw.cmd -B '-Dtest=ExistKorisnikRepositoryIntegrationTest' test
Remove-Item Env:KORISNIK_DB_INTEGRATION_TEST
```

Test očekuje tačno jedan uspešan upis i jedan duplikat, zatim čita nalog po
normalizovanom e-mailu. Razvojni nalog ostaje u bazi; hash se ne ispisuje.
Test je podrazumevano isključen. Nema login-a ni javnog čitanja korisničkog XML-a.
