import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { ReactiveFormsModule } from '@angular/forms';
import { SviZahteviComponent } from './svi-zahtevi.component';
import { PregledZahtevaComponent } from '../pregled-zahteva/pregled-zahteva.component';

describe('Official A1 requests', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<SviZahteviComponent>;
  const document = (id: string) => `<a:zahtevA1 xmlns:a="https://example.org/autorska-prava/a1"
    id="http://localhost:8080/api/a1/zahtevi/${id}">
    <a:podaciGradjanina>
      <a:podnosilac><a:pravnoLice><a:poslovnoIme>Primer DOO</a:poslovnoIme></a:pravnoLice></a:podnosilac>
      <a:delo><a:naslov>Naslov ${id}</a:naslov><a:vrsta>Tekst</a:vrsta><a:formaZapisa>Rukopis</a:formaZapisa>
      <a:autorstvo><a:anonimniAutor/></a:autorstvo></a:delo>
    </a:podaciGradjanina></a:zahtevA1>`;
  const list = (content: string) => `<l:listaZahteva xmlns:l="https://example.org/autorska-prava/a1-lista">${content}</l:listaZahteva>`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule, ReactiveFormsModule],
      declarations: [SviZahteviComponent, PregledZahtevaComponent],
      providers: [{
        provide: ActivatedRoute,
        useValue: {
          snapshot: { data: { roles: ['SLUZBENIK'], listRoute: '/sluzbenik/zahtevi', listTitle: 'Svi zahtevi' } },
          paramMap: of(convertToParamMap({ documentId: 'old-one' }))
        }
      }]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { fixture?.destroy(); http.verify(); });

  it('loads XML from the official endpoint and displays all summaries, including ownerless documents', () => {
    fixture = TestBed.createComponent(SviZahteviComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Učitavanje zahteva');
    const call = http.expectOne('/api/sluzbenik/a1/zahtevi');
    expect(call.request.method).toBe('GET');
    expect(call.request.headers.get('Accept')).toBe('application/xml');
    expect(call.request.responseType).toBe('text');
    expect(call.request.params.keys()).toEqual([]);
    call.flush(list(document('old-one') + document('two')));
    fixture.detectChanges();
    expect(fixture.componentInstance.zahtevi.length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Naslov old-one');
    expect(fixture.nativeElement.textContent).toContain('Vrsta dela: Tekst');
    expect(fixture.nativeElement.querySelector('.button-link').getAttribute('href')).toBe('/sluzbenik/zahtevi/old-one');
  });

  it('displays an empty list', () => {
    fixture = TestBed.createComponent(SviZahteviComponent);
    fixture.detectChanges();
    http.expectOne('/api/sluzbenik/a1/zahtevi').flush(list(''));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nema podnetih zahteva.');
    expect(fixture.componentInstance.loading).toBeFalse();
  });

  it('displays database errors instead of an empty-state message', () => {
    fixture = TestBed.createComponent(SviZahteviComponent);
    fixture.detectChanges();
    http.expectOne('/api/sluzbenik/a1/zahtevi').flush(
      '<greska><status>503</status><poruka>Baza nije dostupna.</poruka></greska>',
      { status: 503, statusText: 'Unavailable' }
    );
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Baza nije dostupna.');
    expect(fixture.nativeElement.textContent).not.toContain('Nema podnetih zahteva.');
    expect(fixture.componentInstance.loading).toBeFalse();
  });

  it('rejects invalid XML responses using the existing list parser', () => {
    fixture = TestBed.createComponent(SviZahteviComponent);
    fixture.detectChanges();
    http.expectOne('/api/sluzbenik/a1/zahtevi').flush('<listaZahteva xmlns="wrong"/>');
    fixture.detectChanges();
    expect(fixture.componentInstance.error).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
  });

  it('reuses the existing details GET and returns to the official list', () => {
    const detail = TestBed.createComponent(PregledZahtevaComponent);
    detail.detectChanges();
    http.expectOne('/api/a1/zahtevi/old-one').flush(document('old-one'));
    http.expectOne('/api/a1/zahtevi/old-one/resenje').flush('', { status: 404, statusText: 'Not Found' });
    detail.detectChanges();
    expect(detail.nativeElement.textContent).toContain('Primer DOO');
    expect(detail.nativeElement.textContent).toContain('Naslov old-one');
    expect(detail.nativeElement.querySelector('a').getAttribute('href')).toBe('/sluzbenik/zahtevi');
    expect(detail.nativeElement.querySelector('a').textContent).toContain('Svi zahtevi');
    detail.destroy();
  });
});
