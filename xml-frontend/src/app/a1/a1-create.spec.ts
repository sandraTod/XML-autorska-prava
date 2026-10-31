import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { A1Service } from './a1.service';
import { A1Podnosenje } from './a1.model';

describe('A1 XML generation and submission', () => {
  let service: A1Service;
  let http: HttpTestingController;
  const namespace = 'https://example.org/autorska-prava/a1';

  function request(): A1Podnosenje {
    return {
      podnosilac: {
        fizickoLice: {
          ime: 'Ana', prezime: 'Primer', drzavljanstvo: 'Srbija',
          adresa: { ulica: 'Primer', broj: '1', mesto: 'Novi Sad', drzava: 'Srbija' }
        }
      },
      delo: {
        naslov: 'Delo', vrsta: 'Tekst', formaZapisa: 'Rukopis',
        autorstvo: { anonimniAutor: true, poznatiAutori: [] }
      },
      potpis: { imePotpisnika: 'Ana', prezimePotpisnika: 'Primer', referenca: 'urn:potpis' },
      prilozi: []
    };
  }

  function parse(value: A1Podnosenje): Document {
    return new DOMParser().parseFromString(service.toXml(value), 'application/xml');
  }

  function names(element: Element): string[] {
    return Array.from(element.children).map(child => child.localName);
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(A1Service);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('generates physical persons with correct namespaces and XSD order, omitting empty optional values', () => {
    const value = request();
    value.podnosilac.kontakt = { telefon: ' ', email: '' };
    value.delo.alternativniNaslov = '  ';
    const document = parse(value);
    expect(document.getElementsByTagName('parsererror').length).toBe(0);
    expect(Array.from(document.getElementsByTagName('*')).every(element => element.namespaceURI === namespace)).toBeTrue();
    expect(document.documentElement.localName).toBe('zahtevA1');
    expect(document.documentElement.hasAttribute('id')).toBeFalse();
    expect(document.documentElement.hasAttribute('vlasnikId')).toBeFalse();
    expect(document.getElementsByTagNameNS(namespace, 'sluzbeniDeo').length).toBe(0);
    expect(names(document.getElementsByTagNameNS(namespace, 'podaciGradjanina')[0])).toEqual(['podnosilac', 'delo', 'potpis']);
    expect(names(document.getElementsByTagNameNS(namespace, 'fizickoLice')[0])).toEqual(['ime', 'prezime', 'adresa', 'drzavljanstvo']);
    expect(names(document.getElementsByTagNameNS(namespace, 'adresa')[0])).toEqual(['ulica', 'broj', 'mesto', 'drzava']);
    expect(names(document.getElementsByTagNameNS(namespace, 'delo')[0])).toEqual(['naslov', 'vrsta', 'formaZapisa', 'autorstvo']);
    for (const name of ['pravnoLice', 'kontakt', 'punomocnik', 'prilozi', 'alternativniNaslov', 'stvorenoURadnomOdnosu']) {
      expect(document.getElementsByTagNameNS(namespace, name).length).toBe(0);
    }
  });

  it('generates legal applicants, proxy, original work, living/deceased authors and attachments in order', () => {
    const value = request();
    const person = value.podnosilac.fizickoLice!;
    value.podnosilac = {
      pravnoLice: { poslovnoIme: 'Primer DOO', sediste: { ...person.adresa!, postanskiBroj: '21000' } },
      kontakt: { telefon: '123', email: 'primer@example.org' }
    };
    value.punomocnik = { ime: 'Mila', prezime: 'Primer', adresa: person.adresa };
    value.delo.alternativniNaslov = 'Drugi naslov';
    value.delo.izvornoDelo = {
      naslov: 'Izvorno', autorstvo: { anonimniAutor: true, poznatiAutori: [] }
    };
    value.delo.autorstvo = {
      anonimniAutor: false,
      poznatiAutori: [
        { ziviAutor: person, pseudonimIliZnak: 'Znak' },
        { preminuliAutor: { ime: 'Petar', prezime: 'Primer', godinaSmrti: '2020' } }
      ]
    };
    value.delo.stvorenoURadnomOdnosu = false;
    value.delo.nacinKoriscenja = 'Objavljivanje';
    value.prilozi = [{ naziv: 'Primer', tip: 'Tekst', referenca: 'urn:primer' }];
    const document = parse(value);
    expect(names(document.getElementsByTagNameNS(namespace, 'podaciGradjanina')[0])).toEqual(['podnosilac', 'punomocnik', 'delo', 'potpis', 'prilozi']);
    expect(names(document.getElementsByTagNameNS(namespace, 'podnosilac')[0])).toEqual(['pravnoLice', 'kontakt']);
    expect(names(document.getElementsByTagNameNS(namespace, 'delo')[0])).toEqual([
      'naslov', 'alternativniNaslov', 'izvornoDelo', 'vrsta', 'formaZapisa', 'autorstvo', 'stvorenoURadnomOdnosu', 'nacinKoriscenja'
    ]);
    expect(names(document.getElementsByTagNameNS(namespace, 'sediste')[0])).toEqual(['ulica', 'broj', 'mesto', 'postanskiBroj', 'drzava']);
    const authors = document.getElementsByTagNameNS(namespace, 'autor');
    expect(authors.length).toBe(2);
    expect(names(authors[0])).toEqual(['ziviAutor', 'pseudonimIliZnak']);
    expect(names(authors[1])).toEqual(['preminuliAutor']);
    expect(document.getElementsByTagNameNS(namespace, 'stvorenoURadnomOdnosu')[0].textContent).toBe('false');
    expect(names(document.getElementsByTagNameNS(namespace, 'prilog')[0])).toEqual(['naziv', 'tip', 'referenca']);
    expect(document.getElementsByTagNameNS(namespace, 'fizickoLice').length).toBe(0);
  });

  it('escapes special characters, preserves text, and supports applicant authorship and true employment', () => {
    const value = request();
    const text = '  Naslov & <tekst> "navod"\r\nnovi red  ';
    value.delo.naslov = text;
    value.delo.autorstvo = { podnosilacJeAutor: { pseudonimIliZnak: 'A & B' }, anonimniAutor: false, poznatiAutori: [] };
    value.delo.stvorenoURadnomOdnosu = true;
    value.potpis.referenca = 'https://example.org/potpis?a=1&b=2';
    const document = parse(value);
    expect(document.getElementsByTagNameNS(namespace, 'naslov')[0].textContent).toBe(text);
    expect(document.getElementsByTagNameNS(namespace, 'pseudonimIliZnak')[0].textContent).toBe('A & B');
    expect(document.getElementsByTagNameNS(namespace, 'referenca')[0].textContent).toBe(value.potpis.referenca);
    expect(document.getElementsByTagNameNS(namespace, 'stvorenoURadnomOdnosu')[0].textContent).toBe('true');
    expect(document.getElementsByTagNameNS(namespace, 'anonimniAutor').length).toBe(0);
    expect(document.getElementsByTagNameNS(namespace, 'poznatiAutori').length).toBe(0);
  });

  it('POSTs XML and reads the document ID only from a successful 201 XML response', () => {
    service.create(request()).subscribe(saved => expect(saved.documentId).toBe('created-one'));
    const call = http.expectOne('/api/a1/zahtevi');
    expect(call.request.method).toBe('POST');
    expect(call.request.headers.get('Content-Type')).toBe('application/xml');
    expect(call.request.responseType).toBe('text');
    const document = new DOMParser().parseFromString(call.request.body, 'application/xml');
    document.documentElement.setAttribute('id', 'http://localhost:8080/api/a1/zahtevi/created-one');
    call.flush(new XMLSerializer().serializeToString(document), { status: 201, statusText: 'Created' });
    service.create(request()).subscribe({ error: error => expect(error.status).toBe(400) });
    http.expectOne('/api/a1/zahtevi').flush('<greska><status>400</status><poruka>Neispravan XML.</poruka></greska>', { status: 400, statusText: 'Bad Request' });
  });
});
