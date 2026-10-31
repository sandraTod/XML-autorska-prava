import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { A1Service } from '../a1.service';
import { A1Zahtev } from '../a1.model';
import { AuthService } from '../../auth/auth.service';

@Component({ selector: 'app-pregled-zahteva', templateUrl: './pregled-zahteva.component.html' })

export class PregledZahtevaComponent implements OnInit, OnDestroy {
  loading = true;
  error = '';
  zahtev: A1Zahtev | null = null;
  private routeSubscription?: Subscription;
  private requestSubscription?: Subscription;

  constructor(private service: A1Service, private auth: AuthService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.routeSubscription = this.route.paramMap.subscribe(params => {
      this.requestSubscription?.unsubscribe();
      this.zahtev = null;
      this.error = '';
      this.loading = true;
      try {
        this.requestSubscription = this.service.findById(params.get('documentId') || '').subscribe({
          next: zahtev => { this.zahtev = zahtev; this.loading = false; },
          error: error => { this.error = this.auth.errorMessage(error); this.loading = false; }
        });
      } catch {
        this.error = 'Neispravan identifikator zahteva.';
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSubscription?.unsubscribe();
    this.requestSubscription?.unsubscribe();
  }
}
