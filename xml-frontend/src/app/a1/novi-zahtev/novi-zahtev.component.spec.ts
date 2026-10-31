import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { NoviZahtevComponent } from './novi-zahtev.component';
import { A1Service } from '../a1.service';

describe('New A1 form', () => {
  let fixture: ComponentFixture<NoviZahtevComponent>;
  let component: NoviZahtevComponent;
  let http: HttpTestingController;
  const address = { ulica: 'Ulica', broj: '1', mesto: 'Novi Sad', drzava: 'Srbija' };
  const person = { ime: 'Ana', prezime: 'Primer', adresa: address, drzavljanstvo: 'Srbija' };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, HttpClientTestingModule, RouterTestingModule],
      declarations: [NoviZahtevComponent]
    }).compileComponents();
    fixture = TestBed.createComponent(NoviZahtevComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    component.form.patchValue({
      podnosilac: { fizickoLice: person },
      delo: { naslov: 'Naslov', vrsta: 'Tekst', formaZapisa: 'Rukopis' },
      potpis: { imePotpisnika: 'Ana', prezimePotpisnika: 'Primer', referenca: 'urn:potpis' }
    });
    fixture.detectChanges();
  });

  afterEach(() => { fixture.destroy(); http.verify(); });

  it('validates only active applicant, proxy and original-work branches, preserving hidden values', () => {
    expect(component.form.valid).toBeTrue();
    component.form.controls['podnosilacTip'].setValue('pravnoLice');
    fixture.detectChanges();
    expect(component.form.invalid).toBeTrue();
    component.form.get('podnosilac.pravnoLice')!.patchValue({ poslovnoIme: 'Primer DOO', sediste: address });
    expect(component.form.valid).toBeTrue();
    expect(component.request().podnosilac.fizickoLice).toBeUndefined();
    component.form.controls['imaPunomocnika'].setValue(true);
    fixture.detectChanges();
    expect(component.form.invalid).toBeTrue();
    component.form.controls['imaPunomocnika'].setValue(false);
    expect(component.form.valid).toBeTrue();
    expect(component.request().punomocnik).toBeUndefined();
    component.form.get('delo.imaIzvornoDelo')!.setValue(true);
    fixture.detectChanges();
    expect(component.form.invalid).toBeTrue();
    component.form.get('delo.izvornoDelo.naslov')!.setValue('Izvorno');
    expect(component.form.valid).toBeTrue();
    component.form.get('delo.imaIzvornoDelo')!.setValue(false);
    expect(component.request().delo.izvornoDelo).toBeUndefined();
    component.form.controls['podnosilacTip'].setValue('fizickoLice');
    expect(component.request().podnosilac.fizickoLice?.ime).toBe('Ana');
  });

  it('supports multiple authors, living/deceased validation, and prevents a legal applicant being the author', () => {
    const group = component.workAuthorship;
    group.controls['tip'].setValue('poznatiAutori');
    fixture.detectChanges();
    expect(component.form.invalid).toBeTrue();
    const first = component.authors(group).at(0) as FormGroup;
    first.controls['ziviAutor'].patchValue(person);
    expect(component.form.valid).toBeTrue();
    component.addAuthor(group);
    const second = component.authors(group).at(1) as FormGroup;
    second.controls['tip'].setValue('preminuliAutor');
    second.controls['preminuliAutor'].patchValue({ ime: 'Petar', prezime: 'Primer', godinaSmrti: '2020' });
    fixture.detectChanges();
    expect(component.form.valid).toBeTrue();
    const authors = component.request().delo.autorstvo.poznatiAutori;
    expect(authors.length).toBe(2);
    expect(authors[0].preminuliAutor).toBeUndefined();
    expect(authors[1].ziviAutor).toBeUndefined();
    second.get('preminuliAutor.godinaSmrti')!.setValue('0000');
    expect(component.form.invalid).toBeTrue();
    group.controls['tip'].setValue('anonimniAutor');
    expect(component.form.valid).toBeTrue();
    expect(component.request().delo.autorstvo.poznatiAutori).toEqual([]);
    group.controls['tip'].setValue('podnosilacJeAutor');
    component.form.controls['podnosilacTip'].setValue('pravnoLice');
    expect(group.controls['tip'].value).toBe('anonimniAutor');
  });

  it('omits empty optional values, distinguishes false from unanswered and requires each added attachment', () => {
    const service = TestBed.inject(A1Service);
    expect(component.request().delo.stvorenoURadnomOdnosu).toBeUndefined();
    component.form.get('delo.stvorenoURadnomOdnosu')!.setValue('false');
    expect(component.request().delo.stvorenoURadnomOdnosu).toBeFalse();
    component.form.patchValue({ podnosilac: { kontakt: { telefon: '  ', email: '' } }, delo: { alternativniNaslov: '  ' } });
    expect(service.toXml(component.request())).not.toContain('<kontakt');
    expect(service.toXml(component.request())).not.toContain('<alternativniNaslov');
    component.addAttachment();
    fixture.detectChanges();
    expect(component.form.invalid).toBeTrue();
    component.attachments.at(0).patchValue({ naziv: 'Primer', tip: 'Tekst', referenca: 'urn:primer' });
    expect(component.form.valid).toBeTrue();
    component.attachments.removeAt(0);
    expect(component.request().prilozi).toEqual([]);
    component.form.get('delo.naslov')!.setValue('   ');
    component.submit();
    http.expectNone('/api/a1/zahtevi');
  });

  it('prevents duplicate submit, preserves values on error and opens the saved request after 201', () => {
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    component.submit();
    component.submit();
    const call = http.expectOne('/api/a1/zahtevi');
    expect(component.pending).toBeTrue();
    call.flush('<greska><status>503</status><poruka>Baza nije dostupna.</poruka></greska>', { status: 503, statusText: 'Unavailable' });
    expect(component.pending).toBeFalse();
    expect(component.error).toBe('Baza nije dostupna.');
    expect(component.form.get('delo.naslov')!.value).toBe('Naslov');
    component.submit();
    const retry = http.expectOne('/api/a1/zahtevi');
    const document = new DOMParser().parseFromString(retry.request.body, 'application/xml');
    document.documentElement.setAttribute('id', 'http://localhost:8080/api/a1/zahtevi/new-one');
    retry.flush(new XMLSerializer().serializeToString(document), { status: 201, statusText: 'Created' });
    expect(navigate).toHaveBeenCalledWith(['/moji-zahtevi', 'new-one']);
    expect(component.pending).toBeFalse();
  });
});
