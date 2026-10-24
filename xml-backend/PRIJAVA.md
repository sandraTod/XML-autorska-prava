# Lokalna prijava i JWT izdavanje

## Veza sa organizacijom prethodnog projekta

Referentni `security` folder projekta `event_planner` služi kao primer organizacije.
Ovde zadržavamo dogovorene pakete i razdvajamo iste odgovornosti:

| Prethodni projekat | Ovaj projekat |
| --- | --- |
| `AuthenticationRestController` | `controller.PrijavaController`: XML ulaz/izlaz i HTTP status |
| Provera naloga i lozinke kroz authentication manager | `service.PrijavaService` / `service.impl.PrijavaServiceImpl`: repository, JAXB validacija i BCrypt |
| `JwtAuthenticationRequest` | XSD-generisani `model.prijava.PrijavaType` |
| `JwtAuthenticationResponse` | XSD-generisani `model.prijavaodgovor.PrijavaOdgovorType` |
| Deo `JwtTokenUtil` za izdavanje | `service.JwtTokenService` / `service.impl.JwtTokenServiceImpl` |
| Konfiguracija ključa | `security.JwtConfiguration`, razvojni ključ iz `application.properties` |

Koristi se constructor injection. Ne dodajemo paralelni kontroler, ručne kopije
JAXB modela, `UserDetails` adaptere ili authentication manager dok za njima nema
potrebe. Iz referentnog projekta se ne prenose ispisi kredencijala, refresh
funkcije, pravila za ignorisanje isteka, aktivacija naloga, CORS politika,
Kod za filter i `SecurityFilterChain` nije kopiran iz reference; prilagođena
implementacija za ovaj projekat opisana je u `SECURITY.md`.

## Tok prijave

`POST /api/auth/prijava` prima XML, validira ga prema `prijava.xsd`, normalizuje
e-mail i traži nalog kroz `KorisnikRepository.findByEmail`. Lozinka se ne trimuje
niti menja. BCrypt proverava sačuvani hash. Nepostojeći nalog i pogrešna lozinka
imaju istu XML poruku i status 401: `Neispravan email ili lozinka.`

Uspeh vraća 200 i XML validiran prema `prijava-odgovor.xsd`, uz
`Cache-Control: no-store`. Odgovor sadrži samo `token` i `expiresAt`.
JJWT 0.13.0 potpisuje HS256 access token sa claim-ovima `sub` (URL naloga),
`uloga` (vrednost iz baze), `iat` i `exp`. Važenje je tačno 24 sata.
Refresh token ne postoji. Token se ne ispisuje u logove.

## Ključ

`application.properties` sadrži `jwt.secret-base64`: razvojni Base64 ključ
generisan iz 32 kriptografski nasumična bajta (256 bita) za HS256.
Označen je kao razvojni i namenjen samo lokalnom razvoju.
`JWT_SECRET_BASE64` više nije potrebna i ne koristi se za ovaj property.
U Eclipse-u možeš ukloniti tu environment promenljivu ako je ranije dodata.
Ključ se ne generiše ponovo pri pokretanju. Njegova promena utiče na proveru
potpisa prethodno izdatih tokena. Test pokretanja proverava konfiguraciju iz
properties fajla, dok ciljani JWT testovi koriste zasebne nasumične ključeve.

U istom terminalu, iz `xml-backend`, podesi pristup bazi i pokreni backend:

```powershell
$credential = Get-Credential -UserName admin -Message 'eXist-db pristup'
$env:EXIST_DB_USERNAME = $credential.UserName
$env:EXIST_DB_PASSWORD = $credential.GetNetworkCredential().Password
./mvnw.cmd spring-boot:run
```

Potrebni su eXist-db na 8081 i registrovan nalog, prema `REGISTRACIJA.md`.
Backend koristi Java 17. Ako ga pokrećeš iz IDE-a, promenljive postavi u njegovoj
lokalnoj run konfiguraciji, bez verzionisanja tajni.

## Postman

1. `POST http://localhost:8080/api/auth/prijava`
2. Headers: `Content-Type: application/xml; charset=UTF-8`, `Accept: application/xml`.
3. Body: raw / XML, sa podacima naloga koji već postoji:

```xml
<p:prijava xmlns:p="https://example.org/autorska-prava/prijava">
  <p:email>mila.primer@example.org</p:email>
  <p:lozinka>RazvojTest123!</p:lozinka>
</p:prijava>
```

Očekuj 200, `Cache-Control: no-store`, `token` i `expiresAt`.
Ne uklanjaj razmake iz lozinke ako su bili deo registracije.
Pogrešna lozinka ili nepoznat e-mail: 401 sa istom porukom.
Neispravan XML: 400. Nedostupna baza: 503. Sve navedene greške su XML.
Neispravan sačuvani nalog ili neočekivana interna greška: generički XML 500.

JWT filter sada proverava tokene i štiti A-1 rute prema pravilima u `SECURITY.md`.
Registracija i prijava ostaju javni POST endpoint-i; vlasništvo nad zahtevima
još nije povezano.

## Automatske i ručne provere

```powershell
./mvnw.cmd -B test
```

`PrijavaControllerTest` koristi mock repository, stvarni BCrypt i stvarno JJWT
potpisivanje. Proverava oba tipa uloga, potpis, subject, 24 sata važenja,
usklađenost `expiresAt`, očuvanje lozinke, XML validaciju, DTD zabranu,
iste 401 odgovore, 503 i bezbedne greške za neispravne sačuvane podatke.
Testovi ne proveravaju login sa stvarnom bazom. Za to ručno izvrši Postman
korake uz stvarnu lokalnu bazu i prethodno registrovan razvojni nalog.

Posle zaustavljanja aplikacije možeš ukloniti tajne iz terminala:

```powershell
Remove-Item Env:EXIST_DB_PASSWORD
```
