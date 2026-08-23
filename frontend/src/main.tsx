import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>,
);

// Fade out splash screen after React mounts
requestAnimationFrame(() => {
  const splash = document.getElementById("splash-loader");
  if (splash) {
    splash.classList.add("fade-out");
    setTimeout(() => splash.remove(), 500);
  }
});
