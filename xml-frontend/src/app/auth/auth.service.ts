import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import jwtDecode, { JwtPayload } from 'jwt-decode';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly headers = new HttpHeaders({ 'Content-Type': 'application/xml', Accept: 'application/xml' });

  constructor(private http: HttpClient) {}

  register(ime: string, prezime: string, email: string, lozinka: string): Observable<void> {
    const xml = this.createXml('registracija', { ime, prezime, email, lozinka });
    return this.http.post('/api/auth/registracija', xml, { headers: this.headers, responseType: 'text' })
      .pipe(map(() => undefined));
  }

  login(email: string, lozinka: string): Observable<void> {
    return this.http.post('/api/auth/prijava', this.createXml('prijava', { email, lozinka }),
      { headers: this.headers, responseType: 'text' }).pipe(map(xml => {
        const document = this.parseXml(xml);
        const namespace = 'https://example.org/autorska-prava/prijava-odgovor';
        if (document.documentElement.localName !== 'prijavaOdgovor' || document.documentElement.namespaceURI !== namespace) {
          throw new Error('Neispravan odgovor servera.');
        }
        const token = document.getElementsByTagNameNS(namespace, 'token')[0]?.textContent;
        const expiresAt = document.getElementsByTagNameNS(namespace, 'expiresAt')[0]?.textContent;
        if (!token || !expiresAt || !this.readClaims(token) || !Number.isFinite(Date.parse(expiresAt))
            || Date.parse(expiresAt) <= Date.now()) {
          throw new Error('Neispravan odgovor servera.');
        }
        sessionStorage.setItem('token', token);
        sessionStorage.setItem('expiresAt', expiresAt);
      }));
  }

  get token(): string | null {
    const token = sessionStorage.getItem('token');
    const expiresAt = sessionStorage.getItem('expiresAt');
    if (!token || !expiresAt || !Number.isFinite(Date.parse(expiresAt))
        || Date.parse(expiresAt) <= Date.now() || !this.readClaims(token)) {
      this.logout();
      return null;
    }
    return token;
  }

  get role(): 'GRADJANIN' | 'SLUZBENIK' | null {
    const token = this.token;
    return token ? this.readClaims(token)?.uloga || null : null;
  }

  logout(): void {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('expiresAt');
  }

  errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) return 'Server nije dostupan. Pokušajte ponovo.';
      try {
        const document = this.parseXml(error.error);
        if (document.documentElement.nodeName === 'greska') {
          return document.getElementsByTagName('poruka')[0]?.textContent || 'Zahtev nije uspeo.';
        }
      } catch {  }
      return 'Zahtev nije uspeo. Pokušajte ponovo.';
    }
    return 'Neispravan odgovor servera. Pokušajte ponovo.';
  }

  private createXml(root: string, fields: { [name: string]: string }): string {
    const namespace = 'https://example.org/autorska-prava/' + root;
    const document = window.document.implementation.createDocument(namespace, root, null);
    Object.keys(fields).forEach(name => {
      const element = document.createElementNS(namespace, name);
      element.textContent = fields[name];
      document.documentElement.appendChild(element);
    });
    return new XMLSerializer().serializeToString(document).replace(/\r/g, '&#13;');
  }

  private parseXml(xml: string): Document {
    if (typeof xml !== 'string' || /<!DOCTYPE/i.test(xml)) throw new Error('Neispravan XML.');
    const document = new DOMParser().parseFromString(xml, 'application/xml');
    if (document.getElementsByTagName('parsererror').length) throw new Error('Neispravan XML.');
    return document;
  }

  private readClaims(token: string): { uloga: 'GRADJANIN' | 'SLUZBENIK' } | null {
    try {
      if (token.split('.').length !== 3) return null;
      const claims = jwtDecode<JwtPayload & { uloga?: unknown }>(token);
      if ((claims.uloga !== 'GRADJANIN' && claims.uloga !== 'SLUZBENIK') || typeof claims.sub !== 'string'
          || !/^https?:\/\//.test(claims.sub) || typeof claims.exp !== 'number' || claims.exp * 1000 <= Date.now()) return null;
      return { uloga: claims.uloga };
    } catch { return null; }
  }
}
