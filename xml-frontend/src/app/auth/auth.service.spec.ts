import { TestBed } from '@angular/core/testing';
import { HttpClient, HTTP_INTERCEPTORS } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';
import { AuthInterceptor } from './auth.interceptor';
import { AuthGuard } from './auth.guard';

describe('XML authentication', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  const expiresAt = () => new Date(Date.now() + 86400000).toISOString();
  const token = (role = 'GRADJANIN', seconds = 86400) => 'header.' + btoa(JSON.stringify({
    sub: 'https://example.org/nalozi/one', uloga: role, exp: Math.floor(Date.now() / 1000) + seconds
  })).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_') + '.signature';

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [{ provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }]
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { http.verify(); sessionStorage.clear(); });

  it('sends namespaced XML with special characters and an unchanged password', () => {
    const password = '  A<&"\r\nB  ';
    auth.register('Ana & Iva', 'Primer', 'ana@example.org', password).subscribe();
    const request = http.expectOne('/api/auth/registracija');
    expect(request.request.headers.get('Content-Type')).toBe('application/xml');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    expect(request.request.responseType).toBe('text');
    const document = new DOMParser().parseFromString(request.request.body, 'application/xml');
    const namespace = 'https://example.org/autorska-prava/registracija';
    expect(document.documentElement.namespaceURI).toBe(namespace);
    expect(document.getElementsByTagNameNS(namespace, 'lozinka')[0].textContent).toBe(password);
    expect(document.getElementsByTagNameNS(namespace, 'ime')[0].textContent).toBe('Ana & Iva');
    request.flush(null, { status: 201, statusText: 'Created' });
  });

  it('reads both JWT roles and expiry from XML and removes an expired session', () => {
    for (const role of ['GRADJANIN', 'SLUZBENIK'] as const) {
      const jwt = token(role);
      const expires = expiresAt();
      auth.login('ana@example.org', ' password ').subscribe();
      const request = http.expectOne('/api/auth/prijava');
      expect(request.request.headers.has('Authorization')).toBeFalse();
      request.flush(`<p:prijavaOdgovor xmlns:p="https://example.org/autorska-prava/prijava-odgovor"><p:token>${jwt}</p:token><p:expiresAt>${expires}</p:expiresAt></p:prijavaOdgovor>`);
      expect(auth.role).toBe(role);
      expect(sessionStorage.getItem('expiresAt')).toBe(expires);
      expect(auth.token).toBe(jwt);
    }
    sessionStorage.setItem('expiresAt', new Date(0).toISOString());
    expect(auth.token).toBeNull();
    expect(sessionStorage.getItem('token')).toBeNull();
  });

  it('rejects malformed login responses and unknown roles', () => {
    let failed = false;
    auth.login('ana@example.org', 'password').subscribe({ error: () => failed = true });
    http.expectOne('/api/auth/prijava').flush('<broken>');
    expect(failed).toBeTrue();
    expect(auth.token).toBeNull();
    sessionStorage.setItem('token', token('ADMIN'));
    sessionStorage.setItem('expiresAt', expiresAt());
    expect(auth.role).toBeNull();
  });

  it('adds a token only to our protected API and clears the session on 401', () => {
    const jwt = token();
    sessionStorage.setItem('token', jwt);
    sessionStorage.setItem('expiresAt', expiresAt());
    const client = TestBed.inject(HttpClient);
    const navigation = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    client.get('/api/a1/zahtevi/one').subscribe({ error: () => undefined });
    const request = http.expectOne('/api/a1/zahtevi/one');
    expect(request.request.headers.get('Authorization')).toBe('Bearer ' + jwt);
    request.flush('', { status: 401, statusText: 'Unauthorized' });
    expect(auth.token).toBeNull();
    expect(navigation).toHaveBeenCalledWith(['/auth/prijava']);
    sessionStorage.setItem('token', jwt);
    sessionStorage.setItem('expiresAt', expiresAt());
    client.get('https://other.example/api/data').subscribe();
    const external = http.expectOne('https://other.example/api/data');
    expect(external.request.headers.has('Authorization')).toBeFalse();
    external.flush({});
  });

  it('guards routes by session and role and supports logout', () => {
    const guard = TestBed.inject(AuthGuard);
    const route = new ActivatedRouteSnapshot();
    route.data = { roles: ['SLUZBENIK'] };
    expect(guard.canActivate(route) instanceof UrlTree).toBeTrue();
    sessionStorage.setItem('token', token());
    sessionStorage.setItem('expiresAt', expiresAt());
    expect(guard.canActivate(route) instanceof UrlTree).toBeTrue();
    route.data = { roles: ['GRADJANIN'] };
    expect(guard.canActivate(route)).toBeTrue();
    auth.logout();
    expect(auth.role).toBeNull();
    expect(sessionStorage.getItem('expiresAt')).toBeNull();
  });
});
