import { NgModule } from '@angular/core';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { AuthInterceptor } from './auth/auth.interceptor';
import { PocetnaComponent } from './pocetna/pocetna.component';
import { BrowserModule } from '@angular/platform-browser';
import { ReactiveFormsModule } from '@angular/forms';
import { NoviZahtevComponent } from './a1/novi-zahtev/novi-zahtev.component';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { MojiZahteviComponent } from './a1/moji-zahtevi/moji-zahtevi.component';
import { PregledZahtevaComponent } from './a1/pregled-zahteva/pregled-zahteva.component';

@NgModule({
  declarations: [
    AppComponent,
    PocetnaComponent,
    MojiZahteviComponent,
    PregledZahtevaComponent,
    NoviZahtevComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    ReactiveFormsModule,
    AppRoutingModule
  ],
  providers: [{ provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }],
  bootstrap: [AppComponent]
})
export class AppModule { }
