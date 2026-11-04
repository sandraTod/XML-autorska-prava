import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject } from 'rxjs';
import { A1Service } from './a1.service';
import { PregledZahtevaComponent } from './pregled-zahteva/pregled-zahteva.component';

describe('Official decisions and resolutions', () => {
  let http: HttpTestingController;
  let service: A1Service;
  let fixture: ComponentFixture<PregledZahtevaComponent> | undefined;
  let route: { snapshot: { data: { roles: string[] } }; paramMap: BehaviorSubject<ReturnType<typeof convertToParamMap>> };
  const requestUrl = '/api/a1/zahtevi/one';
  const resolutionUrl = requestUrl + '/resenje';
  const decisionUrl = '/api/sluzbenik/a1/zahtevi/one/resenje';
  const namespace = 'https://example.org/autorska-prava/resenje';
  const requestXml = `<a:zahtevA1 xmlns:a="https://example.org/autorska-prava/a1" id="http://localhost:8080${requestUrl}">
    <a:podaciGradjanina><a:podnosilac><a:pravnoLice><a:poslovnoIme>Primer DOO</a:poslovnoIme></a:pravnoLice></a:podnosilac>
    <a:delo><a:naslov>Naslov</a:naslov><a:vrsta>Tekst</a:vrsta><a:formaZapisa>Rukopis</a:formaZapisa>
    <a:autorstvo><a:anonimniAutor/></a:autorstvo></a:delo></a:podaciGradjanina></a:zahtevA1>`;
  const resolution = (outcome = '<r:odobrenje><r:sifraDela>AD-one</r:sifraDela></r:odobrenje>') =>
    `<r:resenje xmlns:r="${namespace}" id="http://localhost:8080${resolutionUrl}">
      <r:zahtevRef>http://localhost:8080${requestUrl}</r:zahtevRef><r:datumOdluke>2026-11-03</r:datumOdluke>
      <r:sluzbenik id="https://example.org/nalozi/sluzbenik"><r:ime>Jelena</r:ime><r:prezime>Primer</r:prezime></r:sluzbenik>
      ${outcome}</r:resenje>`;
  const rejection = () => resolution('<r:odbijanje><r:obrazlozenje>Nedostaje prilog &amp; primer.</r:obrazlozenje></r:odbijanje>');

  beforeEach(async () => {
    route = {
      snapshot: { data: { roles: ['SLUZBENIK'] } },
      paramMap: new BehaviorSubject(convertToParamMap({ documentId: 'one' }))
    };
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ReactiveFormsModule, RouterTestingModule],
      declarations: [PregledZahtevaComponent],
      providers: [{ provide: ActivatedRoute, useValue: route }]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(A1Service);
  });
  afterEach(() => { fixture?.destroy(); fixture = undefined; http.verify(); });

  function open(): PregledZahtevaComponent {
    fixture = TestBed.createComponent(PregledZahtevaComponent);
    fixture.detectChanges();
    http.expectOne(requestUrl).flush(requestXml);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  function missing(): PregledZahtevaComponent {
    const component = open();
    http.expectOne(resolutionUrl).flush('', { status: 404, statusText: 'Not Found' });
    fixture!.detectChanges();
    return component;
  }

  it('generates only the decision branch in the correct XML namespace, escaping rejection text', () => {
    const decisionNamespace = 'https://example.org/autorska-prava/odluka';
    const approved = new DOMParser().parseFromString(
      service.decisionToXml({ ishod: 'odobrenje', obrazlozenje: 'Do not send' }), 'application/xml');
    expect(approved.documentElement.localName).toBe('odluka');
    expect(approved.documentElement.attributes.length).toBe(1); // xmlns only
    expect(Array.from(approved.getElementsByTagName('*')).map(element => element.localName)).toEqual(['odluka', 'odobrenje']);
    expect(Array.from(approved.getElementsByTagName('*')).every(element => element.namespaceURI === decisionNamespace)).toBeTrue();
    const text = '  Nedostaje <primer> & "prilog"\r\nnovi red  ';
    const rejected = new DOMParser().parseFromString(service.decisionToXml({ ishod: 'odbijanje', obrazlozenje: text }), 'application/xml');
    expect(rejected.getElementsByTagNameNS(decisionNamespace, 'obrazlozenje')[0].textContent).toBe(text);
    expect(Array.from(rejected.getElementsByTagName('*')).map(element => element.localName)).toEqual(['odluka', 'odbijanje', 'obrazlozenje']);
    expect(() => service.decisionToXml({ ishod: 'odbijanje', obrazlozenje: '  ' })).toThrow();
  });

  it('maps both resolution variants to concrete properties and uses the existing endpoints', () => {
    service.findResolution('one').subscribe(value => {
      expect(value.datumOdluke).toBe('2026-11-03');
      expect(value.sluzbenik.ime).toBe('Jelena');
      expect(value.sluzbenik.prezime).toBe('Primer');
      expect(value.odobrenje?.sifraDela).toBe('AD-one');
    });
    const get = http.expectOne(resolutionUrl);
    expect(get.request.responseType).toBe('text');
    expect(get.request.headers.get('Accept')).toBe('application/xml');
    get.flush(resolution());
    service.createResolution('one', { ishod: 'odbijanje', obrazlozenje: 'Razlog' }).subscribe(value => {
      expect(value.odbijanje?.obrazlozenje).toBe('Nedostaje prilog & primer.');
      expect(value.odobrenje).toBeUndefined();
    });
    const post = http.expectOne(decisionUrl);
    expect(post.request.method).toBe('POST');
    expect(post.request.headers.get('Content-Type')).toBe('application/xml');
    post.flush(rejection(), { status: 201, statusText: 'Created' });
  });

  it('displays existing approval/rejection fields without a new-decision form', () => {
    const component = open();
    expect(component.resolutionLoading).toBeTrue();
    http.expectOne(resolutionUrl).flush(rejection());
    fixture!.detectChanges();
    const text = fixture!.nativeElement.textContent;
    expect(text).toContain('2026-11-03');
    expect(text).toContain('Jelena');
    expect(text).toContain('Primer');
    expect(text).toContain('Zahtev je odbijen');
    expect(text).toContain('Nedostaje prilog & primer.');
    expect(fixture!.nativeElement.querySelector('form')).toBeNull();
    component.submitDecision();
    http.expectNone(decisionUrl);
  });

  it('opens a form only on 404 and requires a nonblank reason only for rejection', () => {
    const component = missing();
    expect(fixture!.nativeElement.querySelector('form')).toBeTruthy();
    expect(component.decisionForm.valid).toBeTrue();
    component.decisionForm.controls['ishod'].setValue('odbijanje');
    component.decisionForm.controls['obrazlozenje'].setValue('   ');
    component.submitDecision();
    http.expectNone(decisionUrl);
    expect(component.decisionForm.invalid).toBeTrue();
    component.decisionForm.controls['ishod'].setValue('odobrenje');
    expect(component.decisionForm.valid).toBeTrue();
    component.submitDecision();
    component.submitDecision();
    expect(component.decisionPending).toBeTrue();
    const post = http.expectOne(decisionUrl);
    expect(post.request.body).not.toContain('obrazlozenje');
    post.flush(resolution(), { status: 201, statusText: 'Created' });
    fixture!.detectChanges();
    expect(component.decisionPending).toBeFalse();
    expect(fixture!.nativeElement.textContent).toContain('AD-one');
    expect(fixture!.nativeElement.querySelector('form')).toBeNull();
  });

  it('keeps the entered rejection after a POST error and displays the returned rejection after retry', () => {
    const component = missing();
    component.decisionForm.setValue({ ishod: 'odbijanje', obrazlozenje: 'Razlog & dokaz' });
    component.submitDecision();
    http.expectOne(decisionUrl).flush('<greska><poruka>Baza nije dostupna.</poruka></greska>', { status: 503, statusText: 'Unavailable' });
    fixture!.detectChanges();
    expect(component.decisionForm.controls['obrazlozenje'].value).toBe('Razlog & dokaz');
    expect(component.decisionError).toBe('Baza nije dostupna.');
    expect(component.decisionPending).toBeFalse();
    expect(fixture!.nativeElement.querySelector('form')).toBeTruthy();
    component.submitDecision();
    http.expectOne(decisionUrl).flush(rejection(), { status: 201, statusText: 'Created' });
    fixture!.detectChanges();
    expect(fixture!.nativeElement.querySelector('form')).toBeNull();
    expect(fixture!.nativeElement.textContent).toContain('Zahtev je odbijen');
  });

  it('reloads an existing resolution on 409 while retaining the conflict message and input', () => {
    const component = missing();
    component.decisionForm.setValue({ ishod: 'odbijanje', obrazlozenje: 'Moj razlog' });
    component.submitDecision();
    http.expectOne(decisionUrl).flush('<greska><poruka>Odluka je već doneta.</poruka></greska>', { status: 409, statusText: 'Conflict' });
    expect(component.resolutionMissing).toBeFalse();
    http.expectOne(resolutionUrl).flush(resolution());
    fixture!.detectChanges();
    expect(fixture!.nativeElement.textContent).toContain('Odluka je već doneta.');
    expect(fixture!.nativeElement.textContent).toContain('AD-one');
    expect(component.decisionForm.controls['obrazlozenje'].value).toBe('Moj razlog');
    expect(fixture!.nativeElement.querySelector('form')).toBeNull();
  });

  it('never treats 503 or invalid saved XML as a missing resolution', () => {
    const component = open();
    http.expectOne(resolutionUrl).flush('<greska><poruka>Baza nije dostupna.</poruka></greska>', { status: 503, statusText: 'Unavailable' });
    fixture!.detectChanges();
    expect(component.resolutionMissing).toBeFalse();
    expect(component.resolutionError).toBe('Baza nije dostupna.');
    expect(fixture!.nativeElement.querySelector('form')).toBeNull();
    route.paramMap.next(convertToParamMap({ documentId: 'one' }));
    http.expectOne(requestUrl).flush(requestXml);
    http.expectOne(resolutionUrl).flush('<resenje xmlns="wrong"/>');
    fixture!.detectChanges();
    expect(component.resolutionError).toBeTruthy();
    expect(component.resolutionMissing).toBeFalse();
    expect(fixture!.nativeElement.querySelector('form')).toBeNull();
  });

  it('does not load resolutions or show decisions in the citizen details view', () => {
    route.snapshot.data.roles = ['GRADJANIN'];
    const component = open();
    http.expectNone(resolutionUrl);
    expect(component.isOfficial).toBeFalse();
    component.submitDecision();
    http.expectNone(decisionUrl);
    expect(fixture!.nativeElement.textContent).not.toContain('Odluka i rešenje');
  });
});
