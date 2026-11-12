import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { A1Service } from '../a1.service';
import { A1Zahtev, A1Resenje } from '../a1.model';
import { AuthService } from '../../auth/auth.service';

@Component({ selector: 'app-pregled-zahteva', templateUrl: './pregled-zahteva.component.html' })

export class PregledZahtevaComponent implements OnInit, OnDestroy {
  loading = true;
  error = '';
  downloadPending = false;
  downloadError = '';
  resolutionDownloadPending = false;
  resolutionDownloadError = '';
  listRoute: string = this.route.snapshot.data['listRoute'] || '/moji-zahtevi';
  listTitle: string = this.route.snapshot.data['listTitle'] || 'Moji zahtevi';
  zahtev: A1Zahtev | null = null;
  isOfficial: boolean = this.route.snapshot.data['roles']?.includes('SLUZBENIK') === true;
  resenje: A1Resenje | null = null;
  resolutionLoading = false;
  resolutionMissing = false;
  resolutionError = '';
  decisionError = '';
  decisionPending = false;
  decisionForm = this.fb.group({
    ishod: ['odobrenje', Validators.required],
    obrazlozenje: [{ value: '', disabled: true }, [Validators.required, Validators.pattern(/\S/)]]
  });
  private documentId = '';
  private routeSubscription?: Subscription;
  private requestSubscription?: Subscription;
  private resolutionSubscription?: Subscription;
  private decisionSubscription?: Subscription;
  private outcomeSubscription?: Subscription;
  private downloadSubscription?: Subscription;
  private downloadUrl?: string;
  private downloadTimer?: number;
  private resolutionDownloadSubscription?: Subscription;
  private resolutionDownloadUrl?: string;
  private resolutionDownloadTimer?: number;

  constructor(private service: A1Service, private auth: AuthService, private route: ActivatedRoute, private fb: FormBuilder) {}

  ngOnInit(): void {
    this.outcomeSubscription = this.decisionForm.controls['ishod'].valueChanges.subscribe(outcome => {
      const reason = this.decisionForm.controls['obrazlozenje'];
      if (outcome === 'odbijanje') reason.enable({ emitEvent: false });
      else reason.disable({ emitEvent: false });
    });
    this.routeSubscription = this.route.paramMap.subscribe(params => {
      this.requestSubscription?.unsubscribe();
      this.resolutionSubscription?.unsubscribe();
      this.decisionSubscription?.unsubscribe();
      this.downloadSubscription?.unsubscribe();
      this.resolutionDownloadSubscription?.unsubscribe();
      this.releaseResolutionDownloadUrl();
      this.resolutionDownloadPending = false;
      this.resolutionDownloadError = '';
      this.releaseDownloadUrl();
      this.downloadPending = false;
      this.downloadError = '';
      this.resenje = null;
      this.resolutionMissing = false;
      this.resolutionLoading = false;
      this.resolutionError = '';
      this.decisionError = '';
      this.decisionPending = false;
      this.decisionForm.reset({ ishod: 'odobrenje', obrazlozenje: '' }, { emitEvent: false });
      this.decisionForm.controls['obrazlozenje'].disable({ emitEvent: false });
      this.documentId = params.get('documentId') || '';
      this.zahtev = null;
      this.error = '';
      this.loading = true;
      try {
        this.requestSubscription = this.service.findById(this.documentId).subscribe({
          next: zahtev => {
            this.zahtev = zahtev;
            this.loading = false;
            this.loadResolution();
          },
          error: error => { this.error = this.auth.errorMessage(error); this.loading = false; }
        });
      } catch {
        this.error = 'Neispravan identifikator zahteva.';
        this.loading = false;
      }
    });
  }

  private loadResolution(allowMissing = true): void {
    this.resolutionSubscription?.unsubscribe();
    this.resolutionMissing = false;
    this.resolutionError = '';
    this.resolutionLoading = true;
    this.resolutionSubscription = this.service.findResolution(this.documentId)
      .pipe(finalize(() => this.resolutionLoading = false)).subscribe({
        next: resenje => this.resenje = resenje,
        error: error => {
          if (allowMissing && error instanceof HttpErrorResponse && error.status === 404) {
            this.resolutionMissing = true;
          } else {
            this.resolutionError = this.auth.errorMessage(error);
          }
        }
      });
  }

  submitDecision(): void {
    if (!this.isOfficial || !this.zahtev || !this.resolutionMissing || this.resenje || this.decisionPending) return;
    this.decisionForm.markAllAsTouched();
    if (this.decisionForm.invalid) return;
    this.decisionError = '';
    this.decisionPending = true;
    this.decisionSubscription = this.service.createResolution(this.documentId, this.decisionForm.value)
      .pipe(finalize(() => this.decisionPending = false)).subscribe({
        next: resenje => {
          this.resenje = resenje;
          this.resolutionMissing = false;
        },
        error: error => {
          this.decisionError = this.auth.errorMessage(error);
          if (error instanceof HttpErrorResponse && error.status === 409) this.loadResolution(false);
        }
      });
  }

  downloadXhtml(): void {
    if (!this.zahtev?.documentId || this.downloadPending) return;
    const documentId = this.zahtev.documentId;
    this.downloadError = '';
    this.downloadPending = true;
    this.downloadSubscription = this.service.downloadXhtml(documentId)
      .pipe(finalize(() => this.downloadPending = false)).subscribe({
        next: blob => {
          this.releaseDownloadUrl();
          this.downloadUrl = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = this.downloadUrl;
          link.download = 'zahtev-' + documentId + '.xhtml';
          document.body.appendChild(link);
          link.click();
          link.remove();
          this.downloadTimer = window.setTimeout(() => this.releaseDownloadUrl(), 0);
        },
        error: error => this.downloadError = this.auth.errorMessage(error)
      });
  }

  private releaseDownloadUrl(): void {
    if (this.downloadTimer !== undefined) window.clearTimeout(this.downloadTimer);
    if (this.downloadUrl) URL.revokeObjectURL(this.downloadUrl);
    this.downloadTimer = undefined;
    this.downloadUrl = undefined;
  }

  downloadResolutionXhtml(): void {
    if (!this.resenje || !this.zahtev?.documentId || this.resolutionDownloadPending) return;
    const documentId = this.zahtev.documentId;
    this.resolutionDownloadError = '';
    this.resolutionDownloadPending = true;
    this.resolutionDownloadSubscription = this.service.downloadResolutionXhtml(documentId)
      .pipe(finalize(() => this.resolutionDownloadPending = false)).subscribe({
        next: blob => {
          this.releaseResolutionDownloadUrl();
          this.resolutionDownloadUrl = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = this.resolutionDownloadUrl;
          link.download = 'resenje-' + documentId + '.xhtml';
          document.body.appendChild(link);
          link.click();
          link.remove();
          this.resolutionDownloadTimer = window.setTimeout(() => this.releaseResolutionDownloadUrl(), 0);
        },
        error: error => this.resolutionDownloadError = this.auth.errorMessage(error)
      });
  }

  private releaseResolutionDownloadUrl(): void {
    if (this.resolutionDownloadTimer !== undefined) window.clearTimeout(this.resolutionDownloadTimer);
    if (this.resolutionDownloadUrl) URL.revokeObjectURL(this.resolutionDownloadUrl);
    this.resolutionDownloadTimer = undefined;
    this.resolutionDownloadUrl = undefined;
  }

  ngOnDestroy(): void {
    this.resolutionDownloadSubscription?.unsubscribe();
    this.releaseResolutionDownloadUrl();
    this.downloadSubscription?.unsubscribe();
    this.releaseDownloadUrl();
    this.routeSubscription?.unsubscribe();
    this.requestSubscription?.unsubscribe();
    this.resolutionSubscription?.unsubscribe();
    this.decisionSubscription?.unsubscribe();
    this.outcomeSubscription?.unsubscribe();
  }
}
