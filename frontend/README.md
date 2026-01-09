# Pizza Frontend

Simple web interface for visualizing the pizza order system in real-time.

## Features

- 📝 Place new pizza orders
- 🚀 View recent orders in real-time
- 🚚 Monitor active deliveries
- 📊 Live statistics dashboard

## Technology Stack

- Node.js 18
- Express.js
- Vanilla JavaScript (no frameworks)
- HTML5 & CSS3

## Running Locally

### Install Dependencies
```bash
npm install
```

### Start the Server
```bash
npm start
```

The frontend will be available at `http://localhost:3000`

## Running with Docker

### Build the Image
```bash
docker build -t pizza-frontend .
```

### Run the Container
```bash
docker run -p 3000:3000 pizza-frontend
```

## Environment Variables

- `PORT` - Server port (default: 3000)

## API Endpoints

The frontend connects to:
- Order Service: `http://localhost:8080`
- Delivery Service: `http://localhost:8083`

## Development

The frontend is intentionally kept simple with no build process or frameworks. It uses:
- Express.js for serving static files
- Vanilla JavaScript for interactivity
- CSS for styling

This makes it easy to understand and modify without complex tooling.
