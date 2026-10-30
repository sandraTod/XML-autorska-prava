import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-pocetna',
  template: `<section class="card"><h1>Početna stranica</h1>
    <ng-container *ngIf="auth.role; else expired">
      <p>Uspešno ste prijavljeni.</p><p>Uloga: <strong>{{ auth.role === 'SLUZBENIK' ? 'Službenik' : 'Građanin' }}</strong></p>
      <button type="button" (click)="logout()">Odjavi se</button>
    </ng-container>
    <ng-template #expired><p>Prijava je istekla.</p><a routerLink="/auth/prijava">Prijavite se ponovo</a></ng-template>
  </section>`
})
export class PocetnaComponent {
  constructor(public auth: AuthService, private router: Router) {}
  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/auth/prijava']);
  }
}
