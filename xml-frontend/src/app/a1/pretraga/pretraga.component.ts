import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs';
import { A1Pregled, A1ZavedenoDelo } from '../a1.model';
import { A1Service } from '../a1.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-pretraga',
  templateUrl: './pretraga.component.html'
})
export class PretragaComponent implements OnInit, OnDestroy {
  form = this.fb.group({ q: [''], tip: ['prihvacena'] });
  loading = false;
  searched = false;
  error = '';
  showingPending = false;
  prihvacenaDela: A1ZavedenoDelo[] = [];
  zahteviNaCekanju: A1Pregled[] = [];
  private requestSubscription?: Subscription;
  private typeSubscription?: Subscription;

  constructor(private fb: FormBuilder, private service: A1Service, private auth: AuthService) {}

  get isOfficial(): boolean {
    return this.auth.role === 'SLUZBENIK';
  }

  ngOnInit(): void {
    this.typeSubscription = this.form.controls['tip'].valueChanges.subscribe(() => this.clearResults());
  }

  search(): void {
    this.clearResults();
    this.searched = true;
    this.loading = true;
    const q: string = this.form.controls['q'].value || '';
    this.showingPending = this.isOfficial && this.form.controls['tip'].value === 'naCekanju';
    if (this.showingPending) {
      this.requestSubscription = this.service.searchPending(q).subscribe({
        next: zahtevi => { this.zahteviNaCekanju = zahtevi; this.loading = false; },
        error: error => { this.error = this.auth.errorMessage(error); this.loading = false; }
      });
    } else {
      this.requestSubscription = this.service.searchAccepted(q).subscribe({
        next: dela => { this.prihvacenaDela = dela; this.loading = false; },
        error: error => { this.error = this.auth.errorMessage(error); this.loading = false; }
      });
    }
  }

  private clearResults(): void {
    this.requestSubscription?.unsubscribe();
    this.prihvacenaDela = [];
    this.zahteviNaCekanju = [];
    this.loading = false;
    this.searched = false;
    this.error = '';
    this.showingPending = false;
  }

  ngOnDestroy(): void {
    this.requestSubscription?.unsubscribe();
    this.typeSubscription?.unsubscribe();
  }
}
