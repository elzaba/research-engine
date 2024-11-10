import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../services/api.service';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css']
})
export class SearchComponent implements OnInit {
  query: string = '';
  pageSize: number = 10;
  proximity: boolean = false;
  proximityDistance: number = 4;
  results: any[] = [];
  currentPage: number = 0;
  suggestions: string[] = [];
  private searchTerms = new Subject<string>();

  constructor(
    private apiService: ApiService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.query = params['query'] || '';
      this.pageSize = +params['pageSize'] || 10;
      this.proximity = params['proximity'] === 'true';
      this.proximityDistance = +params['proximityDistance'] || 4;
      this.currentPage = +params['page'] || 0;

      // Perform search when initialized with URL params
      if (this.query) {
        this.onSearch(false); // Avoid updating the URL again
      }
    });

    // Auto-complete suggestions
    this.searchTerms.pipe(
      debounceTime(300),           // Wait 300ms after each keystroke
      distinctUntilChanged(),       // Ignore if the next search term is the same as the previous
      switchMap((term: string) => this.apiService.autocompleteSuggestions(term))
    ).subscribe(suggestions => this.suggestions = suggestions);
  }

  onSearch(updateUrl: boolean = true): void {
    this.apiService.searchPapers(this.query, this.currentPage, this.pageSize, this.proximity, this.proximityDistance)
      .subscribe(
        data => this.results = data,
        error => console.error('Error fetching search results:', error)
      );

    if (updateUrl) {
      this.updateUrl();
    }
  }

  changePage(page: number): void {
    this.currentPage = page;
    this.onSearch(); // Update the URL
  }

  updateDistanceValue(event: Event): void {
    this.proximityDistance = (event.target as HTMLInputElement).valueAsNumber;
  }

  toggleProximity(): void {
    this.proximity = !this.proximity;
    if (!this.proximity) {
      this.proximityDistance = 4; // Reset distance if proximity is disabled
    }
  }

  onQueryChange(query: string): void {
    this.query = query;
    this.searchTerms.next(query); // Trigger auto-complete
  }

  private updateUrl(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        query: this.query,
        pageSize: this.pageSize,
        proximity: this.proximity,
        proximityDistance: this.proximityDistance,
        page: this.currentPage
      },
      queryParamsHandling: 'merge'
    });
  }
}
