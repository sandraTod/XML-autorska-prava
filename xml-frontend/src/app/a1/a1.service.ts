import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import {
  A1Pregled, A1Zahtev, A1Podnosilac, A1FizickoLice, A1Adresa,
  A1Autorstvo, A1Prilog, A1Delo, A1Podnosenje, A1Odluka, A1Resenje
} from './a1.model';

@Injectable({ providedIn: 'root' })
export class A1Service {
 
  private readonly namespace = 'https://example.org/autorska-prava/a1';
  private readonly headers = new HttpHeaders({ Accept: 'application/xml' });

  constructor(private http: HttpClient) {}

  findResolution(documentId: string): Observable<A1Resenje> {
    this.checkDocumentId(documentId);
    return this.http.get('/api/a1/zahtevi/' + documentId + '/resenje', {
      headers: this.headers, responseType: 'text'
    }).pipe(map(xml => this.readResolution(xml)));
  }

  createResolution(documentId: string, odluka: A1Odluka): Observable<A1Resenje> {
    this.checkDocumentId(documentId);
    return this.http.post('/api/sluzbenik/a1/zahtevi/' + documentId + '/resenje', this.decisionToXml(odluka), {
      headers: this.headers.set('Content-Type', 'application/xml'),
      responseType: 'text', observe: 'response'
    }).pipe(map(response => {
      if (response.status !== 201 || !response.body) throw new Error('Neispravan odgovor servera.');
      return this.readResolution(response.body);
    }));
  }

  decisionToXml(odluka: A1Odluka): string {
    const namespace = 'https://example.org/autorska-prava/odluka';
    const document = window.document.implementation.createDocument(namespace, 'odluka', null);
    if (odluka.ishod !== 'odobrenje' && odluka.ishod !== 'odbijanje') throw new Error('Neispravna odluka.');
    const outcome = document.createElementNS(namespace, odluka.ishod);
    document.documentElement.appendChild(outcome);
    if (odluka.ishod === 'odbijanje') {
      if (!odluka.obrazlozenje?.trim()) throw new Error('Obrazloženje je obavezno.');
      const reason = document.createElementNS(namespace, 'obrazlozenje');
      reason.textContent = odluka.obrazlozenje;
      outcome.appendChild(reason);
    }
    return new XMLSerializer().serializeToString(document).replace(/\r/g, '&#13;');
  }

  private readResolution(xml: string): A1Resenje {
    const namespace = 'https://example.org/autorska-prava/resenje';
    const root = this.parseRoot(xml, 'resenje', namespace);
    const official = this.required(root, 'sluzbenik', namespace);
    const approval = this.children(root, namespace).find(element => element.localName === 'odobrenje');
    const rejection = this.children(root, namespace).find(element => element.localName === 'odbijanje');
    if (!!approval === !!rejection) throw new Error('Neispravan XML odgovor servera.');
    return {
      id: root.getAttribute('id') || '',
      zahtevRef: this.required(root, 'zahtevRef', namespace).textContent || '',
      datumOdluke: this.required(root, 'datumOdluke', namespace).textContent || '',
      sluzbenik: {
        id: official.getAttribute('id') || '',
        ime: this.required(official, 'ime', namespace).textContent || '',
        prezime: this.required(official, 'prezime', namespace).textContent || ''
      },
      odobrenje: approval ? { sifraDela: this.required(approval, 'sifraDela', namespace).textContent || '' } : undefined,
      odbijanje: rejection ? { obrazlozenje: this.required(rejection, 'obrazlozenje', namespace).textContent || '' } : undefined
    };
  }

  private checkDocumentId(documentId: string): void {
    if (!/^[A-Za-z0-9][A-Za-z0-9_-]{0,99}$/.test(documentId)) throw new Error('Neispravan identifikator zahteva.');
  }

  create(zahtev: A1Podnosenje): Observable<A1Pregled> {
    return this.http.post('/api/a1/zahtevi', this.toXml(zahtev), {
      headers: this.headers.set('Content-Type', 'application/xml'),
      responseType: 'text',
      observe: 'response'
    }).pipe(map(response => {
      if (response.status !== 201 || !response.body) throw new Error('Neispravan odgovor servera.');
      const saved = this.summary(this.parseRoot(response.body, 'zahtevA1', this.namespace));
      if (!saved.documentId) throw new Error('Nedostaje identifikator sačuvanog zahteva.');
      return saved;
    }));
  }

  toXml(zahtev: A1Podnosenje): string {
    const document = window.document.implementation.createDocument(this.namespace, 'zahtevA1', null);
    const data = this.appendElement(document.documentElement, 'podaciGradjanina');
    this.appendApplicant(data, zahtev.podnosilac);
    if (zahtev.punomocnik) {
      const proxy = this.appendElement(data, 'punomocnik');
      this.appendText(proxy, 'ime', zahtev.punomocnik.ime);
      this.appendText(proxy, 'prezime', zahtev.punomocnik.prezime);
      this.appendAddress(proxy, 'adresa', zahtev.punomocnik.adresa);
    }
    this.appendWork(data, zahtev.delo);
    const signature = this.appendElement(data, 'potpis');
    this.appendText(signature, 'imePotpisnika', zahtev.potpis.imePotpisnika);
    this.appendText(signature, 'prezimePotpisnika', zahtev.potpis.prezimePotpisnika);
    this.appendText(signature, 'referenca', zahtev.potpis.referenca);
    this.appendAttachments(data, zahtev.prilozi);
    return new XMLSerializer().serializeToString(document).replace(/\r/g, '&#13;');
  }

  private appendApplicant(parent: Element, applicant: A1Podnosilac): void {
    const element = this.appendElement(parent, 'podnosilac');
    if (applicant.fizickoLice) {
      this.appendPerson(element, 'fizickoLice', applicant.fizickoLice);
    } else if (applicant.pravnoLice) {
      const legal = this.appendElement(element, 'pravnoLice');
      this.appendText(legal, 'poslovnoIme', applicant.pravnoLice.poslovnoIme);
      this.appendAddress(legal, 'sediste', applicant.pravnoLice.sediste);
    } else {
      throw new Error('Nedostaje podnosilac.');
    }
    if (applicant.kontakt?.telefon?.trim() || applicant.kontakt?.email?.trim()) {
      const contact = this.appendElement(element, 'kontakt');
      this.appendOptionalText(contact, 'telefon', applicant.kontakt.telefon);
      this.appendOptionalText(contact, 'email', applicant.kontakt.email);
    }
  }

  private appendAddress(parent: Element, name: string, address: A1Adresa | undefined): void {
    if (!address) throw new Error('Nedostaje adresa.');
    const element = this.appendElement(parent, name);
    this.appendText(element, 'ulica', address.ulica);
    this.appendText(element, 'broj', address.broj);
    this.appendText(element, 'mesto', address.mesto);
    this.appendOptionalText(element, 'postanskiBroj', address.postanskiBroj);
    this.appendText(element, 'drzava', address.drzava);
  }

  private appendPerson(parent: Element, name: string, person: A1FizickoLice): void {
    const element = this.appendElement(parent, name);
    this.appendText(element, 'ime', person.ime);
    this.appendText(element, 'prezime', person.prezime);
    this.appendAddress(element, 'adresa', person.adresa);
    this.appendText(element, 'drzavljanstvo', person.drzavljanstvo);
  }

  private appendWork(parent: Element, work: A1Delo): void {
    const element = this.appendElement(parent, 'delo');
    this.appendText(element, 'naslov', work.naslov);
    this.appendOptionalText(element, 'alternativniNaslov', work.alternativniNaslov);
    if (work.izvornoDelo) {
      const original = this.appendElement(element, 'izvornoDelo');
      this.appendText(original, 'naslov', work.izvornoDelo.naslov);
      this.appendAuthorship(original, work.izvornoDelo.autorstvo);
    }
    this.appendText(element, 'vrsta', work.vrsta);
    this.appendText(element, 'formaZapisa', work.formaZapisa);
    this.appendAuthorship(element, work.autorstvo);
    if (work.stvorenoURadnomOdnosu !== undefined) {
      this.appendText(element, 'stvorenoURadnomOdnosu', String(work.stvorenoURadnomOdnosu));
    }
    this.appendOptionalText(element, 'nacinKoriscenja', work.nacinKoriscenja);
  }

  private appendAuthorship(parent: Element, authorship: A1Autorstvo): void {
    const element = this.appendElement(parent, 'autorstvo');
    if (authorship.podnosilacJeAutor) {
      const applicant = this.appendElement(element, 'podnosilacJeAutor');
      this.appendOptionalText(applicant, 'pseudonimIliZnak', authorship.podnosilacJeAutor.pseudonimIliZnak);
    } else if (authorship.anonimniAutor) {
      this.appendElement(element, 'anonimniAutor');
    } else {
      if (!authorship.poznatiAutori.length) throw new Error('Nedostaju autori.');
      const authors = this.appendElement(element, 'poznatiAutori');
      authorship.poznatiAutori.forEach(author => {
        const item = this.appendElement(authors, 'autor');
        if (author.ziviAutor) {
          this.appendPerson(item, 'ziviAutor', author.ziviAutor);
        } else if (author.preminuliAutor) {
          const deceased = this.appendElement(item, 'preminuliAutor');
          this.appendText(deceased, 'ime', author.preminuliAutor.ime);
          this.appendText(deceased, 'prezime', author.preminuliAutor.prezime);
          this.appendText(deceased, 'godinaSmrti', author.preminuliAutor.godinaSmrti);
        } else {
          throw new Error('Nedostaju podaci autora.');
        }
        this.appendOptionalText(item, 'pseudonimIliZnak', author.pseudonimIliZnak);
      });
    }
  }

  private appendAttachments(parent: Element, attachments: A1Prilog[]): void {
    if (!attachments.length) return;
    const element = this.appendElement(parent, 'prilozi');
    attachments.forEach(attachment => {
      const item = this.appendElement(element, 'prilog');
      this.appendText(item, 'naziv', attachment.naziv);
      this.appendText(item, 'tip', attachment.tip);
      this.appendText(item, 'referenca', attachment.referenca);
    });
  }

  private appendElement(parent: Element, name: string): Element {
    const element = parent.ownerDocument.createElementNS(this.namespace, name);
    parent.appendChild(element);
    return element;
  }

  private appendText(parent: Element, name: string, value: string): void {
    this.appendElement(parent, name).textContent = value;
  }

  private appendOptionalText(parent: Element, name: string, value?: string): void {
    if (value?.trim()) this.appendText(parent, name, value);
  }

  findMyRequests(): Observable<A1Pregled[]> {
    return this.http.get('/api/a1/zahtevi/listaZahteva', { headers: this.headers, responseType: 'text' })
      .pipe(map(xml => this.parseList(xml)));
  }

  findAllRequests(): Observable<A1Pregled[]> {
    return this.http.get('/api/sluzbenik/a1/zahtevi', { headers: this.headers, responseType: 'text' })
      .pipe(map(xml => this.parseList(xml)));
  }

  private parseList(xml: string): A1Pregled[] {
    const root = this.parseRoot(xml, 'listaZahteva', 'https://example.org/autorska-prava/a1-lista');
    return this.children(root).map(element => {
      if (element.localName !== 'zahtevA1') throw new Error('Neispravan XML odgovor servera.');
      return this.summary(element);
    });
  }

  findById(documentId: string): Observable<A1Zahtev> {
    if (!/^[A-Za-z0-9][A-Za-z0-9_-]{0,99}$/.test(documentId)) {
      throw new Error('Neispravan identifikator zahteva.');
    }
    return this.http.get('/api/a1/zahtevi/' + encodeURIComponent(documentId),
      { headers: this.headers, responseType: 'text' }).pipe(map(xml => {
        
        const root = this.parseRoot(xml, 'zahtevA1', this.namespace);
        const data = this.required(root, 'podaciGradjanina');
        const proxy = this.optional(data, 'punomocnik');
        const signature = this.optional(data, 'potpis');
        const official = this.optional(root, 'sluzbeniDeo');
        return {
          ...this.summary(root), documentId, id: root.getAttribute('id') || '',
          podnosilac: this.readApplicant(this.required(data, 'podnosilac')),
          punomocnik: proxy ? {
            ime: this.text(proxy, 'ime'), prezime: this.text(proxy, 'prezime'),
            adresa: this.readAddress(this.optional(proxy, 'adresa'))
          } : undefined,
          delo: this.readWork(this.required(data, 'delo')),
          potpis: signature ? {
            imePotpisnika: this.text(signature, 'imePotpisnika'),
            prezimePotpisnika: this.text(signature, 'prezimePotpisnika'),
            referenca: this.text(signature, 'referenca')
          } : undefined,
          prilozi: this.readAttachments(this.optional(data, 'prilozi')),
          sluzbeniDeo: official ? {
            brojPrijave: this.text(official, 'brojPrijave'),
            datumPodnosenja: this.text(official, 'datumPodnosenja'),
            primljeniPrilozi: this.readAttachments(this.optional(official, 'primljeniPrilozi'))
          } : undefined
        };
      }));
  }

  private summary(root: Element): A1Pregled {
    const work = this.required(this.required(root, 'podaciGradjanina'), 'delo');
    let documentId: string | null = null;
    try {
      const url = new URL(root.getAttribute('id') || '');
      const last = url.pathname.split('/').pop() || '';
      if (/^https?:$/.test(url.protocol) && !url.search && !url.hash
          && /^[A-Za-z0-9][A-Za-z0-9_-]{0,99}$/.test(last)) documentId = last;
    } catch {  }
    return {
      documentId,
      naslov: this.required(work, 'naslov').textContent || '',
      vrsta: this.required(work, 'vrsta').textContent || ''
    };
  }

  
  private readApplicant(element: Element): A1Podnosilac {
    const physical = this.optional(element, 'fizickoLice');
    const legal = this.optional(element, 'pravnoLice');
    const contact = this.optional(element, 'kontakt');
    if (!physical && !legal) throw new Error('Neispravan XML odgovor servera.');
    return {
      fizickoLice: physical ? this.readPerson(physical) : undefined,
      pravnoLice: legal ? {
        poslovnoIme: this.text(legal, 'poslovnoIme'),
        sediste: this.readAddress(this.optional(legal, 'sediste'))
      } : undefined,
      kontakt: contact ? {
        telefon: this.optionalText(contact, 'telefon'), email: this.optionalText(contact, 'email')
      } : undefined
    };
  }

  private readPerson(element: Element): A1FizickoLice {
    return {
      ime: this.text(element, 'ime'), prezime: this.text(element, 'prezime'),
      adresa: this.readAddress(this.optional(element, 'adresa')),
      drzavljanstvo: this.text(element, 'drzavljanstvo')
    };
  }

  private readAddress(element: Element | undefined): A1Adresa | undefined {
    if (!element) return undefined;
    return {
      ulica: this.text(element, 'ulica'), broj: this.text(element, 'broj'),
      mesto: this.text(element, 'mesto'), postanskiBroj: this.optionalText(element, 'postanskiBroj'),
      drzava: this.text(element, 'drzava')
    };
  }

  private readWork(element: Element): A1Delo {
    const original = this.optional(element, 'izvornoDelo');
    const employment = this.optionalText(element, 'stvorenoURadnomOdnosu');
    return {
      naslov: this.text(element, 'naslov'), alternativniNaslov: this.optionalText(element, 'alternativniNaslov'),
      vrsta: this.text(element, 'vrsta'), formaZapisa: this.text(element, 'formaZapisa'),
      autorstvo: this.readAuthorship(this.required(element, 'autorstvo')),
      izvornoDelo: original ? {
        naslov: this.text(original, 'naslov'),
        autorstvo: this.readAuthorship(this.required(original, 'autorstvo'))
      } : undefined,
      stvorenoURadnomOdnosu: employment === undefined ? undefined : ['true', '1'].includes(employment.trim()),
      nacinKoriscenja: this.optionalText(element, 'nacinKoriscenja')
    };
  }

  private readAuthorship(element: Element): A1Autorstvo {
    const applicant = this.optional(element, 'podnosilacJeAutor');
    const anonymous = this.optional(element, 'anonimniAutor');
    const known = this.optional(element, 'poznatiAutori');
    if (!applicant && !anonymous && !known) throw new Error('Neispravan XML odgovor servera.');
    return {
      podnosilacJeAutor: applicant ? { pseudonimIliZnak: this.optionalText(applicant, 'pseudonimIliZnak') } : undefined,
      anonimniAutor: !!anonymous,
      poznatiAutori: known ? this.children(known).map(author => {
        const living = this.optional(author, 'ziviAutor');
        const deceased = living ? undefined : this.required(author, 'preminuliAutor');
        return {
          ziviAutor: living ? this.readPerson(living) : undefined,
          preminuliAutor: deceased ? {
            ime: this.text(deceased, 'ime'), prezime: this.text(deceased, 'prezime'),
            godinaSmrti: this.text(deceased, 'godinaSmrti')
          } : undefined,
          pseudonimIliZnak: this.optionalText(author, 'pseudonimIliZnak')
        };
      }) : []
    };
  }

  private readAttachments(element: Element | undefined): A1Prilog[] {
    return element ? this.children(element).map(attachment => ({
      naziv: this.text(attachment, 'naziv'), tip: this.text(attachment, 'tip'),
      referenca: this.text(attachment, 'referenca')
    })) : [];
  }

  private text(parent: Element, name: string): string {
    return this.optionalText(parent, name) || '';
  }

  private optionalText(parent: Element, name: string): string | undefined {
    const element = this.optional(parent, name);
    return element ? element.textContent || '' : undefined;
  }

  private optional(parent: Element, name: string): Element | undefined {
    return this.children(parent).find(element => element.localName === name);
  }

  private children(parent: Element, namespace = this.namespace): Element[] {
    const children = Array.from(parent.children);
    if (children.some(element => element.namespaceURI !== namespace)) {
      throw new Error('Neispravan XML odgovor servera.');
    }
    return children;
  }

  private required(parent: Element, name: string, namespace = this.namespace): Element {
    const element = this.children(parent, namespace).find(child => child.localName === name);
    if (!element) throw new Error('Neispravan XML odgovor servera.');
    return element;
  }

  private parseRoot(xml: string, name: string, namespace: string): Element {
    if (/<!DOCTYPE/i.test(xml)) throw new Error('Neispravan XML odgovor servera.');
    const document = new DOMParser().parseFromString(xml, 'application/xml');
    const root = document.documentElement;
    if (document.getElementsByTagName('parsererror').length || root.localName !== name || root.namespaceURI !== namespace) {
      throw new Error('Neispravan XML odgovor servera.');
    }
    return root;
  }
}
