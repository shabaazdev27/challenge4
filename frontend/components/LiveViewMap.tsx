import React, { useEffect, useRef } from 'react';

declare global {
  interface Window {
    google: any;
    initMap: () => void;
  }
}

interface LiveViewMapProps {
  apiKey: string;
  lat?: number;
  lng?: number;
}

export default function LiveViewMap({ apiKey, lat = 40.8128, lng = -74.0742 }: LiveViewMapProps) {
  const mapRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // @ts-ignore - Google Maps is loaded dynamically
    if (!window.google) {
      const script = document.createElement('script');
      script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&callback=initMap`;
      script.async = true;
      script.defer = true;
      document.head.appendChild(script);

      window.initMap = () => {
        if (mapRef.current) {
          new window.google.maps.StreetViewPanorama(mapRef.current, {
            position: { lat, lng },
            pov: { heading: 165, pitch: 0 },
            zoom: 1,
          });
        }
      };
    } else if (mapRef.current) {
      new window.google.maps.StreetViewPanorama(mapRef.current, {
        position: { lat, lng },
        pov: { heading: 165, pitch: 0 },
        zoom: 1,
      });
    }
  }, [apiKey, lat, lng]);

  return <div ref={mapRef} role="region" aria-label="Interactive street view map" data-testid="stadium-map" style={{ width: '100%', height: '100%', minHeight: '400px' }} />;
}
