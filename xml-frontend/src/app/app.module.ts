import { NgModule } from '@angular/core';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { AuthInterceptor } from './auth/auth.interceptor';
import { PocetnaComponent } from './pocetna/pocetna.component';
import { BrowserModule } from '@angular/platform-browser';
import { ReactiveFormsModule } from '@angular/forms';
import { SviZahteviComponent } from './a1/svi-zahtevi/svi-zahtevi.component';
import { NoviZahtevComponent } from './a1/novi-zahtev/novi-zahtev.component';
import { PretragaComponent } from './a1/pretraga/pretraga.component';

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
    NoviZahtevComponent,
    SviZahteviComponent,
    PretragaComponent
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
