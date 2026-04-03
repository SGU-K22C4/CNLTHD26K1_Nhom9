import { useState, useEffect } from 'react';

// Using Session Storage for caching to improve performance
export const useProvinces = (depth = 2) => {
  const [provinces, setProvinces] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchProvinces = async () => {
      try {
        const cacheKey = `provinces_v2_depth_${depth}`;
        const cachedData = sessionStorage.getItem(cacheKey);

        if (cachedData) {
          setProvinces(JSON.parse(cachedData));
          setLoading(false);
          return;
        }

        const res = await fetch(`https://provinces.open-api.vn/api/v2/?depth=${depth}`);
        const data = await res.json();

        sessionStorage.setItem(cacheKey, JSON.stringify(data));
        setProvinces(data);
        setLoading(false);
      } catch (err) {
        console.error("Error API Provinces v2", err);
        setError(err);
        setLoading(false);
      }
    };

    fetchProvinces();
  }, [depth]);

  return { provinces, loading, error };
};
