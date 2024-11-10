// src/app/app.module.ts
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { IndexComponent } from './index/index.component';
import { SearchComponent } from './search/search.component';
import { ApiService } from './services/api.service';

@NgModule({
  declarations: [
    AppComponent,
    IndexComponent,
    SearchComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    HttpClientModule // Import HttpClientModule for API calls
  ],
  providers: [ApiService],
  bootstrap: [AppComponent]
})
export class AppModule { }
