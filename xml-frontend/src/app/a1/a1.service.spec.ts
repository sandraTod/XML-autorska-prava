import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { A1Service } from './a1.service';
import { A1Zahtev } from './a1.model';

describe('A1 XML service', () => {
  let service: A1Service;
  let http: HttpTestingController;
  const namespace = 'https://example.org/autorska-prava/a1';
  const request = (data = '<a:podnosilac><a:pravnoLice><a:poslovnoIme>Primer &amp; partneri</a:poslovnoIme></a:pravnoLice></a:podnosilac>',
    authors = '<a:anonimniAutor/>') => `<a:zahtevA1 xmlns:a="${namespace}" id="http://localhost:8080/api/a1/zahtevi/doc-one">
    <a:podaciGradjanina>${data}<a:delo><a:naslov>Priča &lt;test&gt;</a:naslov><a:vrsta>Književno delo</a:vrsta>
    <a:formaZapisa>Tekst</a:formaZapisa><a:autorstvo>${authors}</a:autorstvo><a:stvorenoURadnomOdnosu>false</a:stvorenoURadnomOdnosu></a:delo>
    <a:potpis><a:imePotpisnika>Ana</a:imePotpisnika><a:prezimePotpisnika>Primer</a:prezimePotpisnika><a:referenca>potpis:test</a:referenca></a:potpis>
    </a:podaciGradjanina></a:zahtevA1>`;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(A1Service);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('loads summaries without owner parameters, and supports an empty list', () => {
    service.findMyRequests().subscribe(list => {
      expect(list.length).toBe(2);
      expect(list[0]).toEqual({ documentId: 'doc-one', naslov: 'Priča <test>', vrsta: 'Književno delo' });
    });
    const call = http.expectOne('/api/a1/zahtevi/listaZahteva');
    expect(call.request.params.keys()).toEqual([]);
    expect(call.request.responseType).toBe('text');
    expect(call.request.headers.get('Accept')).toBe('application/xml');
    call.flush(`<l:listaZahteva xmlns:l="https://example.org/autorska-prava/a1-lista">${request()}${request()}</l:listaZahteva>`);
    service.findMyRequests().subscribe(list => expect(list).toEqual([]));
    http.expectOne('/api/a1/zahtevi/listaZahteva').flush('<listaZahteva xmlns="https://example.org/autorska-prava/a1-lista"/>');
  });

  it('displays legal persons, anonymous authors and booleans as plain text', () => {
    let result: A1Zahtev | undefined;
    service.findById('doc-one').subscribe(value => result = value);
    http.expectOne('/api/a1/zahtevi/doc-one').flush(request());
    expect(result?.podnosilac.pravnoLice?.poslovnoIme).toBe('Primer & partneri');
    expect(result?.podnosilac.fizickoLice).toBeUndefined();
    expect(result?.delo.autorstvo.anonimniAutor).toBeTrue();
    expect(result?.delo.stvorenoURadnomOdnosu).toBeFalse();

  });

  it('supports physical persons, applicant authors and multiple living/deceased authors', () => {
    const person = '<a:podnosilac><a:fizickoLice><a:ime>Ana</a:ime><a:prezime>Primer</a:prezime><a:adresa><a:mesto>Novi Sad</a:mesto></a:adresa></a:fizickoLice></a:podnosilac>';
    const authors = '<a:poznatiAutori><a:autor><a:ziviAutor><a:ime>Ana</a:ime></a:ziviAutor><a:pseudonimIliZnak>A</a:pseudonimIliZnak></a:autor><a:autor><a:preminuliAutor><a:ime>Ivan</a:ime><a:godinaSmrti>2000</a:godinaSmrti></a:preminuliAutor></a:autor></a:poznatiAutori>';
    service.findById('doc-one').subscribe(value => {
      expect(value.podnosilac.fizickoLice?.ime).toBe('Ana');
      expect(value.podnosilac.fizickoLice?.adresa?.mesto).toBe('Novi Sad');
      expect(value.podnosilac.pravnoLice).toBeUndefined();
      const authors = value.delo.autorstvo.poznatiAutori;
      expect(authors.length).toBe(2);
      expect(authors[0].ziviAutor?.ime).toBe('Ana');
      expect(authors[0].pseudonimIliZnak).toBe('A');
      expect(authors[1].preminuliAutor?.godinaSmrti).toBe('2000');

    });
    http.expectOne('/api/a1/zahtevi/doc-one').flush(request(person, authors));
    service.findById('doc-one').subscribe(value => {
      expect(value.delo.autorstvo.podnosilacJeAutor).toBeDefined();
      expect(value.delo.autorstvo.anonimniAutor).toBeFalse();
    });
    http.expectOne('/api/a1/zahtevi/doc-one').flush(request(person, '<a:podnosilacJeAutor/>'));
  });

  it('rejects malformed and wrong-namespace XML and propagates 404/503 errors', () => {
    for (const xml of ['<broken>', '<listaZahteva xmlns="wrong"/>', '<listaZahteva xmlns="https://example.org/autorska-prava/a1-lista"><zahtevA1 xmlns="wrong"/></listaZahteva>']) {
      let failed = false;
      service.findMyRequests().subscribe({ error: () => failed = true });
      http.expectOne('/api/a1/zahtevi/listaZahteva').flush(xml);
      expect(failed).toBeTrue();
    }
    for (const status of [404, 503]) {
      service.findById('missing').subscribe({ error: error => expect(error.status).toBe(status) });
      http.expectOne('/api/a1/zahtevi/missing').flush('<greska/>', { status, statusText: 'Error' });
    }
    expect(() => service.findById('../unsafe')).toThrow();
  });

  it('maps original authorship, proxy, attachments and official data to concrete properties', () => {
    const attachment = (name: string) => `<a:prilog><a:naziv>${name}</a:naziv><a:tip>PDF</a:tip><a:referenca>urn:${name}</a:referenca></a:prilog>`;
    const xml = request().replace('<a:vrsta>', '<a:izvornoDelo><a:naslov>Original</a:naslov><a:autorstvo><a:podnosilacJeAutor><a:pseudonimIliZnak>Originalni znak</a:pseudonimIliZnak></a:podnosilacJeAutor></a:autorstvo></a:izvornoDelo><a:vrsta>')
      .replace('<a:delo>', '<a:punomocnik><a:ime>Petar</a:ime><a:prezime>Primer</a:prezime><a:adresa><a:ulica>Ulica 1</a:ulica></a:adresa></a:punomocnik><a:delo>')
      .replace('</a:podaciGradjanina>', `<a:prilozi>${attachment('prvi')}${attachment('drugi')}</a:prilozi></a:podaciGradjanina>`)
      .replace('</a:zahtevA1>', `<a:sluzbeniDeo><a:brojPrijave>A-1/2026</a:brojPrijave><a:datumPodnosenja>2026-10-31</a:datumPodnosenja><a:primljeniPrilozi>${attachment('primljeni')}</a:primljeniPrilozi></a:sluzbeniDeo></a:zahtevA1>`);
    service.findById('doc-one').subscribe(value => {
      expect(value.punomocnik?.ime).toBe('Petar');
      expect(value.punomocnik?.adresa?.ulica).toBe('Ulica 1');
      expect(value.delo.izvornoDelo?.naslov).toBe('Original');
      expect(value.delo.izvornoDelo?.autorstvo.podnosilacJeAutor?.pseudonimIliZnak).toBe('Originalni znak');
      expect(value.prilozi.length).toBe(2);
      expect(value.prilozi[0].referenca).toBe('urn:prvi');
      expect(value.prilozi[1].referenca).toBe('urn:drugi');
      expect(value.sluzbeniDeo?.brojPrijave).toBe('A-1/2026');
      expect(value.sluzbeniDeo?.datumPodnosenja).toBe('2026-10-31');
      expect(value.sluzbeniDeo?.primljeniPrilozi[0].naziv).toBe('primljeni');
      expect(value.potpis?.imePotpisnika).toBe('Ana');
      expect(value.potpis?.referenca).toBe('potpis:test');

    });
    http.expectOne('/api/a1/zahtevi/doc-one').flush(xml);
  });
});
