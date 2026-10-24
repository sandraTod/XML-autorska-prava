# JWT autentifikacija postojećih ruta

Security klase su u paketu `com.example.xml_backend.security`:
`JwtConfiguration`, `JwtAuthenticationFilter`, `WebSecurityConfig` i
`XmlSecurityErrorHandler`. Postojeći `JwtTokenService` i njegova implementacija
ostaju u `service` / `service.impl`; servis sada i proverava token.

Koristi se postojeći razvojni ključ `jwt.secret-base64` iz
`application.properties`, bez ispisivanja u logove. Token se izdaje na 24 sata.
Nema refresh tokena. Parser dozvoljava samo HS256, proverava potpis i istek,
zahteva `exp`, HTTP(S) URL subject i ulogu `GRADJANIN` ili `SLUZBENIK`.
Nepoznata/nedostajuća uloga znači neispravan token (401).

Filter čita isključivo `Authorization: Bearer <token>` i postavlja principal
na URL naloga, uz `ROLE_GRADJANIN` ili `ROLE_SLUZBENIK` authority.
Kreiran je samo u security lancu i nije automatski registrovan kao servlet filter.
Ne postoje sesije, form login, HTTP Basic, autentifikacija preko kolačića ili
automatski generisan Spring korisnik. CSRF je isključen jer je autentifikacija
isključivo kroz Bearer zaglavlje, koje pregledač ne šalje automatski kao kolačić.

| Ruta | Pristup |
| --- | --- |
| POST `/api/auth/registracija` | Javno, bez tokena |
| POST `/api/auth/prijava` | Javno, bez tokena |
| POST `/api/a1/zahtevi` | GRADJANIN |
| GET `/api/a1/zahtevi/{documentId}` | GRADJANIN ili SLUZBENIK |
| Sve ostalo | Zabranjeno |

Nedostajući token na zaštićenoj ruti ili neispravan/istekao token daju 401.
Validan token sa nedovoljnom dozvolom daje 403. Ako se Authorization zaglavlje
pošalje i javnoj ruti, mora biti validno. Odgovori su XML:

```xml
<greska><status>401</status><poruka>Potrebna je važeća prijava.</poruka></greska>
```

Za 403 poruka je `Pristup nije dozvoljen.` Nema internih detalja.
Poslovni XML odgovori (400, 404, 503 i dr.) ostaju isti nakon uspešne security provere.
Ovo proverava samo potpis, prijavu i ulogu iz tokena; vlasništvo nad zahtevom,
promena uloge posle izdavanja i opoziv tokena nisu dodati.

## Postman

1. Pokreni bazu na 8081 i backend na 8080, sa `EXIST_DB_PASSWORD` u Eclipse-u.
2. Za registraciju i prijavu izaberi **Authorization → No Auth**. Pošalji XML
   prijavu na `POST http://localhost:8080/api/auth/prijava` sa podacima postojećeg
   naloga. Postavi `Content-Type: application/xml` i `Accept: application/xml`.
3. Iz XML odgovora kopiraj sadržaj elementa `token`.
4. Za A-1 rute izaberi **Authorization → Bearer Token** i nalepi samo vrednost
   tokena. Pošalji primer A-1 XML-a kao građanin na `POST /api/a1/zahtevi`.
   Očekuj 201 i Location; GET te adrese vraća 200 i XML.
5. Bez tokena ili sa izmenjenim/isteklim tokenom očekuj XML 401.
6. Token već predefinisanog službenika dozvoljava GET, a POST vraća XML 403.
   Nije dodato kreiranje službenika. Nepostojeći dokument uz validan token vraća 404.

## Automatske provere

```powershell
./mvnw.cmd -B clean test
```

`JwtSecurityTest` koristi pun Spring Security lanac, stvarne XML/login servise,
BCrypt i JWT potpise, ali mock repository-je. Pokriva javne rute, odsutan,
neispravan, nepotpisan, istekao i token drugog algoritma/ključa, obavezne claim-ove,
obe uloge, zabranu drugih ruta, XML greške, odsustvo sesije i jednu registraciju filtera.
Testovi ne potvrđuju rad sa stvarnom bazom. Za tu proveru koristi Postman
uz nalog prethodno sačuvan u lokalnom eXist-db-u.
