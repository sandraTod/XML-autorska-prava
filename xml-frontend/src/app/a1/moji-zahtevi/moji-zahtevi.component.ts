import { Component, OnInit } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { A1Service } from '../a1.service';
import { A1Pregled } from '../a1.model';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-moji-zahtevi',
  templateUrl: './moji-zahtevi.component.html'
})
export class MojiZahteviComponent implements OnInit {
  loading = true;
  error = '';
  zahtevi: A1Pregled[] = [];

  constructor(private service: A1Service, private auth: AuthService) {}

  ngOnInit(): void {
    this.service.findMyRequests().pipe(finalize(() => this.loading = false)).subscribe({
      next: zahtevi => this.zahtevi = zahtevi,
      error: error => this.error = this.auth.errorMessage(error)
    });
  }
}
