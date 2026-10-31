import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import {
  A1Pregled, A1Zahtev, A1Podnosilac, A1FizickoLice, A1Adresa,
  A1Autorstvo, A1Prilog, A1Delo
} from './a1.model';

@Injectable({ providedIn: 'root' })
export class A1Service {
 
  private readonly namespace = 'https://example.org/autorska-prava/a1';
  private readonly headers = new HttpHeaders({ Accept: 'application/xml' });

  constructor(private http: HttpClient) {}

  findMyRequests(): Observable<A1Pregled[]> {
    return this.http.get('/api/a1/zahtevi/listaZahteva', { headers: this.headers, responseType: 'text' })
      .pipe(map(xml => {
        const root = this.parseRoot(xml, 'listaZahteva', 'https://example.org/autorska-prava/a1-lista');
        return this.children(root).map(element => {
          if (element.localName !== 'zahtevA1') throw new Error('Neispravan XML odgovor servera.');
          return this.summary(element);
        });
      }));
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

  private children(parent: Element): Element[] {
    const children = Array.from(parent.children);
    if (children.some(element => element.namespaceURI !== this.namespace)) {
      throw new Error('Neispravan XML odgovor servera.');
    }
    return children;
  }

  private required(parent: Element, name: string): Element {
    const element = this.children(parent).find(child => child.localName === name);
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
