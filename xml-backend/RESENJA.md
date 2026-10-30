# Razvojna provera odluke i rešenja

Backend koristi postojeći JWT i postojeće naloge. Potrebni su pokrenut
eXist-db i `EXIST_DB_PASSWORD` u Eclipse Run Configuration. Priprema službenika
ostaje opisana u `SLUZBENIK.md`.

## Postman

1. Prijavite građanina preko `POST http://localhost:8080/api/auth/prijava`
   i njegovim Bearer tokenom kreirajte dva različita A-1 zahteva preko
   `POST /api/a1/zahtevi`. Sačuvajte njihove `documentId` vrednosti.
2. Prijavite službenika istim login endpointom. Za naredni POST postavite
   njegov token u Authorization → Bearer Token i `Content-Type: application/xml`.
3. Pošaljite `POST http://localhost:8080/api/sluzbenik/a1/zahtevi/{documentId}/resenje`.
   Body → raw → XML: sadržaj `src/main/resources/odluka-odobrenje-primer.xml`.
   Očekuje se 201, `Location` i validno XML rešenje sa šifrom `AD-{documentId}`.
4. Za drugi zahtev pošaljite `odluka-odbijanje-primer.xml`. Očekuje se 201
   i obrazloženje u rešenju. Izostavljeno ili prazno obrazloženje daje 400.
5. Ponovite bilo koju odluku nad prvim zahtevom: očekuje se 409, prethodno
   rešenje ostaje nepromenjeno. Dva istovremena POST-a za NOV zahtev moraju
   dati jedan 201 i jedan 409.
6. `GET http://localhost:8080/api/a1/zahtevi/{documentId}/resenje` sa tokenom
   vlasnika ili službenika daje 200. Drugi građanin dobija 404; građanin za
   dokument bez vlasnika takođe dobija 404. Bez tokena je 401; građanin na
   službeničkom POST-u dobija 403. Odsutno rešenje/zahtev daje 404.

Backend uzima ID iz autentifikacije, a identitet i ulogu službenika iz
sačuvanog naloga. Neispravan sačuvani XML daje 503, a ne 404. Rešenja su
u `/db/autorska-prava/resenja/{documentId}.xml`; A-1 se ne menja.

## Testovi

`./mvnw.cmd test` pokreće automatske testove sa zamenjenim repository-jima
i lokalnim HTTP stubom. Oni ne dokazuju ponašanje zaključavanja stvarne baze.

Opcioni integracioni test proverava dva istovremena upisa preko dve repository
instance: tačno jedan uspeva, drugi prijavljuje konflikt, A-1 ostaje nepromenjen.
Za njega u lokalnom PowerShell-u podesite:

```powershell
$env:RESENJE_DB_INTEGRATION_TEST = 'true'
# EXIST_DB_PASSWORD postavite lokalno; ne upisujte je u projektne fajlove.
.\mvnw.cmd '-Dtest=ExistResenjeRepositoryIntegrationTest' test
```

Podrazumevani REST URL je `http://localhost:8081/exist/rest`, korisnik `admin`;
opciono se menjaju kroz `EXIST_DB_URL` i `EXIST_DB_USERNAME`. Test ostavlja dva
razvojna dokumenta sa jedinstvenim imenima u bazi. Bez navedene zastavice se
preskače. Eclipse environment nije automatski dostupan zasebnom PowerShell-u.

Provera postojanja i upis su unutar `util:exclusive-lock` nad konkretnim A-1
dokumentom, prema [zvaničnoj dokumentaciji eXist-db](https://exist-db.org/exist/apps/fundocs/view?uri=http%3A%2F%2Fexist-db.org%2Fxquery%2Futil).
XQuery koristi spoljne parametre. Greške pristupa/konekcije se ne pretvaraju
u odsutan dokument. Kolekcija rešenja se kreira tek tokom prvog upisa.
