import { Component, OnDestroy } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { A1Autorstvo, A1Podnosenje, A1AutorUnos } from '../a1.model';
import { A1Service } from '../a1.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-novi-zahtev',
  templateUrl: './novi-zahtev.component.html'
})
export class NoviZahtevComponent implements OnDestroy {
  pending = false;
  error = '';
  submitted = false;
  private readonly subscriptions = new Subscription();
  private readonly requiredText = [Validators.required, Validators.pattern(/\S/)];

  form = this.fb.group({
    podnosilacTip: ['fizickoLice'],
    podnosilac: this.fb.group({
      fizickoLice: this.createPerson(),
      pravnoLice: this.fb.group({
        poslovnoIme: ['', this.requiredText],
        sediste: this.createAddress()
      }),
      kontakt: this.fb.group({ telefon: [''], email: [''] })
    }),
    imaPunomocnika: [false],
    punomocnik: this.fb.group({
      ime: ['', this.requiredText],
      prezime: ['', this.requiredText],
      adresa: this.createAddress()
    }),
    delo: this.fb.group({
      naslov: ['', this.requiredText],
      alternativniNaslov: [''],
      imaIzvornoDelo: [false],
      izvornoDelo: this.fb.group({ naslov: ['', this.requiredText], autorstvo: this.createAuthorship() }),
      vrsta: ['', this.requiredText],
      formaZapisa: ['', this.requiredText],
      autorstvo: this.createAuthorship(),
      stvorenoURadnomOdnosu: [''],
      nacinKoriscenja: ['']
    }),
    potpis: this.fb.group({
      imePotpisnika: ['', this.requiredText],
      prezimePotpisnika: ['', this.requiredText],
      referenca: ['', this.requiredText]
    }),
    prilozi: this.fb.array([])
  });

  constructor(
    private fb: FormBuilder,
    private service: A1Service,
    private auth: AuthService,
    private router: Router
  ) {
    this.syncApplicant();
    this.form.controls['punomocnik'].disable({ emitEvent: false });
    this.form.get('delo.izvornoDelo')!.disable({ emitEvent: false });
    this.subscriptions.add(this.form.controls['podnosilacTip'].valueChanges.subscribe(() => this.syncApplicant()));
    this.subscriptions.add(this.form.controls['imaPunomocnika'].valueChanges.subscribe(enabled => {
      if (enabled) this.form.controls['punomocnik'].enable({ emitEvent: false });
      else this.form.controls['punomocnik'].disable({ emitEvent: false });
    }));
    this.subscriptions.add(this.form.get('delo.imaIzvornoDelo')!.valueChanges.subscribe(enabled => {
      const original = this.form.get('delo.izvornoDelo')!;
      if (enabled) {
        original.enable({ emitEvent: false });
        this.syncAuthorship(this.originalAuthorship);
      } else original.disable({ emitEvent: false });
    }));
  }

  get isPhysical(): boolean {
    return this.form.controls['podnosilacTip'].value === 'fizickoLice';
  }

  get workAuthorship(): FormGroup {
    return this.form.get('delo.autorstvo') as FormGroup;
  }

  get originalAuthorship(): FormGroup {
    return this.form.get('delo.izvornoDelo.autorstvo') as FormGroup;
  }

  get attachments(): FormArray {
    return this.form.controls['prilozi'] as FormArray;
  }

  authors(group: FormGroup): FormArray {
    return group.controls['poznatiAutori'] as FormArray;
  }

  addAuthor(group: FormGroup): void {
    this.authors(group).push(this.createAuthor());
  }

  removeAuthor(group: FormGroup, index: number): void {
    if (this.authors(group).length > 1) this.authors(group).removeAt(index);
  }

  addAttachment(): void {
    this.attachments.push(this.fb.group({
      naziv: ['', this.requiredText], tip: ['', this.requiredText], referenca: ['', this.requiredText]
    }));
  }

  invalid(path: string): boolean {
    const control = this.form.get(path);
    return !!control && control.enabled && control.invalid && (control.touched || this.submitted);
  }

  submit(): void {
    if (this.pending) return;
    this.submitted = true;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.error = '';
    this.pending = true;
    this.subscriptions.add(this.service.create(this.request()).pipe(finalize(() => this.pending = false)).subscribe({
      next: saved => { void this.router.navigate(['/moji-zahtevi', saved.documentId]); },
      error: error => this.error = this.auth.errorMessage(error)
    }));
  }

  request(): A1Podnosenje {
    const value = this.form.value;
    const work = value.delo;
    return {
      podnosilac: {
        fizickoLice: this.isPhysical ? value.podnosilac.fizickoLice : undefined,
        pravnoLice: this.isPhysical ? undefined : value.podnosilac.pravnoLice,
        kontakt: value.podnosilac.kontakt
      },
      punomocnik: value.imaPunomocnika ? value.punomocnik : undefined,
      delo: {
        naslov: work.naslov,
        alternativniNaslov: work.alternativniNaslov,
        izvornoDelo: work.imaIzvornoDelo ? {
          naslov: work.izvornoDelo.naslov,
          autorstvo: this.authorshipValue(this.originalAuthorship)
        } : undefined,
        vrsta: work.vrsta,
        formaZapisa: work.formaZapisa,
        autorstvo: this.authorshipValue(this.workAuthorship),
        stvorenoURadnomOdnosu: work.stvorenoURadnomOdnosu === '' ? undefined : work.stvorenoURadnomOdnosu === 'true',
        nacinKoriscenja: work.nacinKoriscenja
      },
      potpis: value.potpis,
      prilozi: value.prilozi
    };
  }

  private authorshipValue(group: FormGroup): A1Autorstvo {
    const value = group.value;
    return {
      podnosilacJeAutor: value.tip === 'podnosilacJeAutor' ? { pseudonimIliZnak: value.pseudonimIliZnak } : undefined,
      anonimniAutor: value.tip === 'anonimniAutor',
      poznatiAutori: value.tip === 'poznatiAutori' ? value.poznatiAutori.map((author: A1AutorUnos) => ({
        ziviAutor: author.tip === 'ziviAutor' ? author.ziviAutor : undefined,
        preminuliAutor: author.tip === 'preminuliAutor' ? author.preminuliAutor : undefined,
        pseudonimIliZnak: author.pseudonimIliZnak
      })) : []
    };
  }

  private createAddress(): FormGroup {
    return this.fb.group({
      ulica: ['', this.requiredText], broj: ['', this.requiredText], mesto: ['', this.requiredText],
      postanskiBroj: [''], drzava: ['', this.requiredText]
    });
  }

  private createPerson(): FormGroup {
    return this.fb.group({
      ime: ['', this.requiredText], prezime: ['', this.requiredText],
      adresa: this.createAddress(), drzavljanstvo: ['', this.requiredText]
    });
  }

  private createAuthor(): FormGroup {
    const group = this.fb.group({
      tip: ['ziviAutor'],
      ziviAutor: this.createPerson(),
      preminuliAutor: this.fb.group({
        ime: ['', this.requiredText], prezime: ['', this.requiredText],
        godinaSmrti: ['', [Validators.required, this.yearValidator]]
      }),
      pseudonimIliZnak: ['']
    });
    this.syncAuthor(group);
    this.subscriptions.add(group.controls['tip'].valueChanges.subscribe(() => this.syncAuthor(group)));
    return group;
  }

  private createAuthorship(): FormGroup {
    const group = this.fb.group({
      tip: ['anonimniAutor'], pseudonimIliZnak: [''], poznatiAutori: this.fb.array([this.createAuthor()], Validators.required)
    });
    this.syncAuthorship(group);
    this.subscriptions.add(group.controls['tip'].valueChanges.subscribe(() => this.syncAuthorship(group)));
    return group;
  }

  private syncApplicant(): void {
    const applicant = this.form.controls['podnosilac'] as FormGroup;
    if (this.isPhysical) {
      applicant.controls['fizickoLice'].enable({ emitEvent: false });
      applicant.controls['pravnoLice'].disable({ emitEvent: false });
    } else {
      applicant.controls['fizickoLice'].disable({ emitEvent: false });
      applicant.controls['pravnoLice'].enable({ emitEvent: false });
      [this.workAuthorship, this.originalAuthorship].forEach(group => {
        if (group.controls['tip'].value === 'podnosilacJeAutor') {
          group.controls['tip'].setValue('anonimniAutor', { emitEvent: false });
          if (group.enabled) this.syncAuthorship(group);
        }
      });
    }
  }

  private syncAuthorship(group: FormGroup): void {
    const tip = group.controls['tip'].value;
    group.controls['pseudonimIliZnak'].disable({ emitEvent: false });
    group.controls['poznatiAutori'].disable({ emitEvent: false });
    if (tip === 'podnosilacJeAutor') group.controls['pseudonimIliZnak'].enable({ emitEvent: false });
    if (tip === 'poznatiAutori') {
      const authors = this.authors(group);
      authors.enable({ emitEvent: false });
      authors.controls.forEach(author => this.syncAuthor(author as FormGroup));
    }
  }

  private syncAuthor(group: FormGroup): void {
    const living = group.controls['tip'].value === 'ziviAutor';
    if (living) {
      group.controls['ziviAutor'].enable({ emitEvent: false });
      group.controls['preminuliAutor'].disable({ emitEvent: false });
    } else {
      group.controls['ziviAutor'].disable({ emitEvent: false });
      group.controls['preminuliAutor'].enable({ emitEvent: false });
    }
  }

  private yearValidator(control: AbstractControl): ValidationErrors | null {
    const value = String(control.value || '');
    const match = /^(-?(?:\d{4}|[1-9]\d{4,}))(Z|[+-](?:0\d|1[0-4]):[0-5]\d)?$/.exec(value);
    if (!match || /^-?0+$/.test(match[1]) || /[+-]14:(?!00)/.test(value)) return { year: true };
    return null;
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }
}
