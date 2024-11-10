import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../services/api.service';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css']
})
export class SearchComponent implements OnInit {
  query: string = '';
  pageSize: number = 10;
  proximity: boolean = false; // Proximity search toggle
  proximityDistance: number = 4; // Proximity distance value
  results: any[] = [];
  currentPage: number = 0;

  constructor(private apiService: ApiService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.query = params['query'] || '';
      this.pageSize = +params['pageSize'] || 10;
      this.proximity = params['proximity'] === 'true';
      this.proximityDistance = +params['proximityDistance'] || 4;
      this.onSearch();
    });
  }

  onSearch() {
    this.apiService.searchPapers(this.query, this.currentPage, this.pageSize, this.proximity, this.proximityDistance).subscribe(
      (data) => {
        this.results = data;
      },
      (error) => {
        console.error('Error fetching search results:', error);
      }
    );
  }

  changePage(page: number) {
    this.currentPage = page;
    this.onSearch();
  }

  updateDistanceValue(event: Event) {
    this.proximityDistance = (event.target as HTMLInputElement).valueAsNumber;
  }

  toggleProximity() {
    // Reset distance if proximity search is disabled
    if (!this.proximity) {
      this.proximityDistance = 4;
    }
  }
}
