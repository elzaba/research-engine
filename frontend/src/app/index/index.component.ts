// src/app/index/index.component.ts
import { Component } from '@angular/core';
import { ApiService } from '../services/api.service';

@Component({
  selector: 'app-index',
  templateUrl: './index.component.html',
  styleUrls: ['./index.component.css']
})
export class IndexComponent {
  query: string = '';
  pageSize: number = 10;
  proximity: boolean = false;
  proximityDistance: number = 4;

  constructor(private apiService: ApiService) {}

  // Trigger the search operation when the form is submitted
  onSearch() {
    // Call search functionality or navigate to the search component
  }

  // Update the displayed value for proximity range slider
  updateDistanceValue(event: Event) {
    this.proximityDistance = (event.target as HTMLInputElement).valueAsNumber;
  }
}
