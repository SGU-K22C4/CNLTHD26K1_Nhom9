import { Box, List, ListItemText, ListItem } from "@mui/material";
import { Link, useLocation } from "react-router-dom";

function DesktopMenu(props) {
  const { options, setIsHovered, setIsOpen } = props;
  const location = useLocation();

  return (
    <Box sx={{ display: { xs: "none", md: "flex" } }}>
      <List
        sx={{
          display: "flex",
          gap: { md: "24px" },
          p: 0,
        }}
      >
        {options.map((option, index) => {
          const isActive = location.pathname.startsWith(option?.path || '/');
          return (
            <Link
              to={option?.path || '/'}
              key={option?.id || index}
              style={{ textDecoration: 'none', color: 'inherit' }}
              onMouseOver={() => {
                setIsHovered?.(option?.name || '');
                setIsOpen?.(false);
              }}
            >
              <ListItem>
                <ListItemText
                  primary={option?.name}
                  sx={{ color: isActive ? "#748C70" : "inherit" }}
                />
              </ListItem>
            </Link>
          );
        })}
      </List>
    </Box>
  );
}

export default DesktopMenu;
