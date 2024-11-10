import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

interface Paper {
  title: string;
  path: string;
  contents: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // Search Papers
  searchPapers(query: string, page: number = 0, pageSize: number = 10, proximity: boolean = false, proximityDistance: number = 4): Observable<Paper[]> {
    let params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', pageSize.toString())
      .set('proximity', proximity.toString())
      .set('proximityDistance', proximityDistance.toString());

    // Fix the URL string by using backticks
    return this.http.get<Paper[]>(`${this.baseUrl}/search`, { params });
  }

  autocompleteSuggestions(query: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/autocomplete`, {
      params: { query }
    });
  }

  // Index Documents
  indexDocuments(): Observable<string> {
    return this.http.post<string>(`${this.baseUrl}/index`, {});
  }
}
